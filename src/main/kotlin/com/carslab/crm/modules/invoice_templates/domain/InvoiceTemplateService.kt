package com.carslab.crm.modules.invoice_templates.domain

import com.carslab.crm.modules.invoice_templates.api.requests.ActivateTemplateRequest
import com.carslab.crm.modules.invoice_templates.api.requests.UploadTemplateRequest
import com.carslab.crm.modules.invoice_templates.domain.model.*
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.invoice_templates.infrastructure.templates.ProfessionalDefaultTemplateProvider
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import java.util.*

@Service
class InvoiceTemplateService(
    private val templateRepository: InvoiceTemplateRepository,
    private val pdfGenerationService: PdfGenerationService,
    private val templateRenderingService: TemplateRenderingService,
    private val companySettingsService: CompanySettingsDomainService,
    private val logoStorageService: LogoStorageService,
    private val professionalDefaultTemplateProvider: ProfessionalDefaultTemplateProvider,
    private val documentService: UnifiedDocumentService,
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateService::class.java)

    @Transactional(readOnly = true)
    fun generateInvoiceForDocument(
        companyId: Long,
        documentId: String,
        templateId: InvoiceTemplateId? = null
    ): ByteArray {
        logger.debug("Generating invoice for document {} with template {} for company {}",
            documentId, templateId?.value, companyId)

        validateCompanyId(companyId)

        val document = documentService.getDocumentById(documentId)
        val template = getTemplateForGeneration(companyId, templateId)
        val companySettings = getCompanySettings(companyId)
        val logoData = getLogoData(companySettings)

        val generationData = InvoiceGenerationData(
            document = document,
            companySettings = companySettings,
            logoData = logoData
        )

        return generatePdfFromTemplate(template, generationData)
    }

    fun generatePdfFromTemplate(template: InvoiceTemplate, data: InvoiceGenerationData): ByteArray {
        logger.debug("Generating PDF from template: ${template.id.value} for document: ${data.document.id.value}")

        return try {
            val renderedHtml = templateRenderingService.renderTemplate(template, data)
            val pdfBytes = pdfGenerationService.generatePdf(renderedHtml, template.content.layout)

            logger.debug("Successfully generated PDF, size: ${pdfBytes.size} bytes")
            pdfBytes

        } catch (e: Exception) {
            logger.error("Failed to generate PDF from template: ${template.id.value}", e)
            throw RuntimeException("PDF generation failed: ${e.message}", e)
        }
    }

    @Transactional
    fun uploadTemplate(request: UploadTemplateRequest, companyId: Long): InvoiceTemplate {
        logger.debug("Uploading template '{}' for company {}", request.name, companyId)

        validateCompanyId(companyId)
        validateUploadRequest(request)

        val htmlContent = extractHtmlContent(request.file)
        validateHtmlContentForPlaywright(htmlContent)

        val template = createTemplateFromUpload(request, companyId, htmlContent)
        return templateRepository.save(template)
    }

    @Transactional
    fun activateTemplate(companyId: Long, request: ActivateTemplateRequest) {
        logger.debug("Activating template {} for company {}", request.templateId.value, companyId)

        validateCompanyId(companyId)

        val template = templateRepository.findById(request.templateId)
            ?: throw ResourceNotFoundException("Template", request.templateId.value)

        if (!template.canBeUsedBy(companyId)) {
            logger.warn("Company {} attempted to activate template {} which doesn't belong to them",
                companyId, request.templateId.value)
            throw ValidationException("Access denied - template does not belong to your company")
        }

        templateRepository.deactivateAllTemplatesForCompany(companyId)
        val activatedTemplate = template.copy(isActive = true)
        templateRepository.save(activatedTemplate)

        logger.info("Template {} activated for company {}", request.templateId.value, companyId)
    }

    @Transactional(readOnly = true)
    fun generateTemplatePreview(templateId: InvoiceTemplateId, companyId: Long): ByteArray {
        logger.debug("Generating preview for template {} for company {}", templateId.value, companyId)

        validateCompanyId(companyId)

        val template = getTemplateForCompany(templateId, companyId)
        val mockData = createMockInvoiceData(companyId)
        return generatePdfFromTemplate(template, mockData)
    }

    @Transactional(readOnly = true)
    fun exportTemplate(templateId: InvoiceTemplateId, companyId: Long): ByteArray {
        logger.debug("Exporting template {} for company {}", templateId.value, companyId)

        validateCompanyId(companyId)

        val template = getTemplateForCompany(templateId, companyId)
        return template.content.htmlTemplate.toByteArray(Charsets.UTF_8)
    }

    @Transactional
    fun getTemplatesForCompany(companyId: Long): List<InvoiceTemplate> {
        logger.debug("Getting templates for company {}", companyId)

        validateCompanyId(companyId)

        val existingTemplates = templateRepository.findByCompanyId(companyId)

        return if (existingTemplates.isEmpty()) {
            logger.debug("No templates found for company {}, creating default template", companyId)

            val existingDefault = templateRepository.findActiveTemplateForCompany(companyId)
            if (existingDefault != null) {
                logger.debug("Default template already exists for company {}: {}", companyId, existingDefault.id.value)
                return listOf(existingDefault)
            }

            val defaultTemplate = professionalDefaultTemplateProvider.createDefaultTemplate(companyId)
            val savedTemplate = templateRepository.save(defaultTemplate)
            logger.info("Default template created for company {}: {}", companyId, savedTemplate.id.value)
            listOf(savedTemplate)
        } else {
            logger.debug("Found {} existing templates for company {}", existingTemplates.size, companyId)
            existingTemplates
        }
    }

    @Transactional(readOnly = true)
    fun getTemplate(templateId: InvoiceTemplateId, companyId: Long): InvoiceTemplate {
        logger.debug("Getting template {} for company {}", templateId.value, companyId)

        validateCompanyId(companyId)

        return getTemplateForCompany(templateId, companyId)
    }

    @Transactional
    fun deleteTemplate(templateId: InvoiceTemplateId, companyId: Long): Boolean {
        logger.debug("Deleting template {} for company {}", templateId.value, companyId)

        validateCompanyId(companyId)

        val template = templateRepository.findById(templateId) ?: return false

        if (!template.canBeUsedBy(companyId)) {
            logger.warn("Company {} attempted to delete template {} which doesn't belong to them",
                companyId, templateId.value)
            throw ValidationException("Access denied - template does not belong to your company")
        }

        if (template.templateType == TemplateType.SYSTEM_DEFAULT) {
            throw ValidationException("Cannot delete system default template")
        }

        return templateRepository.deleteById(templateId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createDefaultTemplateIfNeeded(companyId: Long): InvoiceTemplate {
        logger.debug("Creating default template for company {}", companyId)

        val existing = templateRepository.findActiveTemplateForCompany(companyId)
        if (existing != null) {
            logger.debug("Default template already exists, returning existing: {}", existing.id.value)
            return existing
        }

        val defaultTemplate = professionalDefaultTemplateProvider.createDefaultTemplate(companyId)
        val savedTemplate = templateRepository.save(defaultTemplate)

        logger.info("Default template created successfully for company {}: {}", companyId, savedTemplate.id.value)
        return savedTemplate
    }

    @Transactional(readOnly = true)
    private fun getTemplateForGeneration(companyId: Long, templateId: InvoiceTemplateId?): InvoiceTemplate {
        return templateId?.let {
            getTemplateForCompany(it, companyId)
        } ?: templateRepository.findActiveTemplateForCompany(companyId)
        ?: getSystemDefaultTemplate()
    }

    @Transactional(readOnly = true)
    private fun getTemplateForCompany(templateId: InvoiceTemplateId, companyId: Long): InvoiceTemplate {
        val template = templateRepository.findById(templateId)
            ?: throw ResourceNotFoundException("Template", templateId.value)

        if (!template.canBeUsedBy(companyId)) {
            logger.warn("Company {} attempted to access template {} which doesn't belong to them",
                companyId, templateId.value)
            throw ValidationException("Access denied - template does not belong to your company")
        }

        return template
    }

    private fun getSystemDefaultTemplate(): InvoiceTemplate {
        return templateRepository.findSystemDefaultTemplate()
            ?: throw IllegalStateException("System default template not found")
    }

    private fun getCompanySettings(companyId: Long) =
        companySettingsService.getCompanySettings(companyId)
            ?: throw IllegalStateException("Company settings not found for company: $companyId")

    private fun getLogoData(companySettings: com.carslab.crm.modules.company_settings.domain.model.CompanySettings): ByteArray? {
        return companySettings.logoSettings.logoFileId?.let { logoFileId ->
            try {
                logoStorageService.getLogoPath(logoFileId)?.let { path ->
                    java.nio.file.Files.readAllBytes(path)
                }
            } catch (e: Exception) {
                logger.warn("Failed to load logo", e)
                null
            }
        }
    }

    private fun validateCompanyId(companyId: Long) {
        if (companyId <= 0) {
            logger.error("Invalid company ID provided: {}", companyId)
            throw ValidationException("Invalid company ID")
        }
    }

    private fun validateUploadRequest(request: UploadTemplateRequest) {
        if (request.file.isEmpty) {
            throw ValidationException("File cannot be empty")
        }

        if (request.file.size > 5 * 1024 * 1024) {
            throw ValidationException("File size cannot exceed 5MB")
        }

        val filename = request.file.originalFilename
        if (filename == null || !filename.endsWith(".html", ignoreCase = true)) {
            throw ValidationException("File must be HTML format")
        }

        if (request.name.isBlank() || request.name.length > 100) {
            throw ValidationException("Template name must be between 1 and 100 characters")
        }
    }

    private fun extractHtmlContent(file: org.springframework.web.multipart.MultipartFile): String {
        return try {
            String(file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw ValidationException("Failed to read HTML content from file: ${e.message}")
        }
    }

    private fun validateHtmlContentForPlaywright(html: String) {
        if (html.isBlank()) {
            throw ValidationException("HTML content cannot be empty")
        }

        if (html.length > 5_000_000) {
            throw ValidationException("HTML content too large (max 5MB)")
        }

        if (!templateRenderingService.validateTemplateSyntax(html)) {
            throw ValidationException("Invalid HTML template syntax")
        }

        logger.debug("HTML template validated for Playwright - no restrictions")
    }

    private fun createTemplateFromUpload(
        request: UploadTemplateRequest,
        companyId: Long,
        htmlContent: String
    ): InvoiceTemplate {
        return InvoiceTemplate(
            id = InvoiceTemplateId.generate(),
            companyId = companyId,
            name = request.name.trim(),
            description = request.description?.trim(),
            templateType = TemplateType.COMPANY_CUSTOM,
            content = TemplateContent(
                htmlTemplate = htmlContent,
                cssStyles = extractCssFromHtml(htmlContent),
                logoPlacement = LogoPlacement(
                    position = LogoPosition.TOP_LEFT,
                    maxWidth = 150,
                    maxHeight = 80
                ),
                layout = LayoutSettings(
                    pageSize = PageSize.A4,
                    margins = Margins(top = 20, right = 20, bottom = 20, left = 20),
                    headerHeight = null,
                    footerHeight = null,
                    fontFamily = "DejaVuSans",
                    fontSize = 12
                )
            ),
            isActive = false,
            metadata = TemplateMetadata(
                version = "2.0",
                author = null,
                tags = setOf("custom", "uploaded", "playwright"),
                legalCompliance = LegalCompliance(
                    country = "PL",
                    vatCompliant = true,
                    requiredFields = emptySet(),
                    lastLegalReview = null
                ),
                supportedLanguages = setOf("pl")
            ),
            audit = com.carslab.crm.domain.model.Audit(
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )
    }

    private fun extractCssFromHtml(html: String): String {
        val stylePattern = """<style[^>]*>(.*?)</style>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = stylePattern.findAll(html)
        return matches.joinToString("\n") { it.groupValues[1] }
    }

    private fun createMockInvoiceData(companyId: Long): InvoiceGenerationData {
        val companySettings = getCompanySettings(companyId)

        val mockDocument = com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument(
            id = com.carslab.crm.domain.model.view.finance.UnifiedDocumentId("preview"),
            number = "PREVIEW/2024/001",
            type = com.carslab.crm.api.model.DocumentType.INVOICE,
            title = "Przykładowa faktura - podgląd szablonu",
            description = "Podgląd szablonu faktury z przykładowymi danymi",
            issuedDate = java.time.LocalDate.now(),
            dueDate = java.time.LocalDate.now().plusDays(14),
            sellerName = companySettings.basicInfo.companyName,
            sellerTaxId = companySettings.basicInfo.taxId,
            sellerAddress = companySettings.basicInfo.address ?: "ul. Przykładowa 1\n00-001 Warszawa",
            buyerName = "Przykładowy Klient Sp. z o.o.",
            buyerTaxId = "1234567890",
            buyerAddress = "ul. Kliencka 123\n00-002 Kraków",
            status = com.carslab.crm.api.model.DocumentStatus.NOT_PAID,
            direction = com.carslab.crm.api.model.TransactionDirection.INCOME,
            paymentMethod = com.carslab.crm.domain.model.view.finance.PaymentMethod.BANK_TRANSFER,
            totalNet = java.math.BigDecimal("1000.00"),
            totalTax = java.math.BigDecimal("230.00"),
            totalGross = java.math.BigDecimal("1230.00"),
            paidAmount = java.math.BigDecimal.ZERO,
            currency = "PLN",
            notes = "Przykładowe uwagi do faktury",
            protocolId = null,
            protocolNumber = null,
            visitId = null,
            items = listOf(
                com.carslab.crm.domain.model.view.finance.DocumentItem(
                    id = "item1",
                    name = "Usługa detailingowa Premium",
                    description = "Kompleksowe czyszczenie zewnętrzne i wewnętrzne pojazdu",
                    quantity = java.math.BigDecimal("1"),
                    unitPrice = java.math.BigDecimal("800.00"),
                    taxRate = java.math.BigDecimal("23"),
                    totalNet = java.math.BigDecimal("800.00"),
                    totalGross = java.math.BigDecimal("984.00")
                ),
                com.carslab.crm.domain.model.view.finance.DocumentItem(
                    id = "item2",
                    name = "Wosk ochronny",
                    description = "Aplikacja wysokiej jakości wosku ochronnego",
                    quantity = java.math.BigDecimal("1"),
                    unitPrice = java.math.BigDecimal("200.00"),
                    taxRate = java.math.BigDecimal("23"),
                    totalNet = java.math.BigDecimal("200.00"),
                    totalGross = java.math.BigDecimal("246.00")
                )
            ),
            attachment = null,
            audit = com.carslab.crm.domain.model.Audit(
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        return InvoiceGenerationData(
            document = mockDocument,
            companySettings = companySettings,
            logoData = getLogoData(companySettings)
        )
    }
}