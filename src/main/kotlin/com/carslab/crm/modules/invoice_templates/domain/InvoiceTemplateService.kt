package com.carslab.crm.modules.invoice_templates.domain

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.modules.invoice_templates.api.requests.ActivateTemplateRequest
import com.carslab.crm.modules.invoice_templates.api.requests.UploadTemplateRequest
import com.carslab.crm.modules.invoice_templates.domain.model.*
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.invoice_templates.infrastructure.templates.ProfessionalDefaultTemplateProvider
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.modules.company_settings.domain.model.BankSettings
import com.carslab.crm.modules.company_settings.domain.model.CompanyBasicInfo
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.modules.company_settings.domain.model.LogoSettings
import com.carslab.crm.modules.company_settings.domain.model.shared.AuditInfo
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateHeaderResponse
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateResponse
import com.carslab.crm.production.modules.invoice_templates.application.service.InvoiceGenerationService
import com.carslab.crm.production.modules.invoice_templates.application.service.InvoiceTemplateQueryService
import com.carslab.crm.production.modules.templates.application.service.query.TemplateQueryService
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class InvoiceTemplateService(
    private val templateRepository: InvoiceTemplateRepository,
    private val pdfGenerationService: PdfGenerationService,
    private val templateRenderingService: TemplateRenderingService,
    private val logoStorageService: LogoStorageService,
    private val professionalDefaultTemplateProvider: ProfessionalDefaultTemplateProvider,
    private val documentService: UnifiedDocumentService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val templateQueryService: InvoiceTemplateQueryService,
    private val templateService: TemplateQueryService, 
    private val templateGenerationService: InvoiceGenerationService,
    private val universalStorageService: UniversalStorageService,
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateService::class.java)

    @Transactional(readOnly = true)
    fun generateInvoiceForDocument(
        documentId: String,
        templateId: InvoiceTemplateId? = null,
        authContext: AuthContext,
    ): ByteArray {
        logger.debug("Generating invoice for document {} with template {} for company {}",
            documentId, templateId?.value, authContext.companyId)

        validateCompanyId(authContext.companyId.value)

        val document = documentService.getDocumentById(documentId, authContext)
        val template: ByteArray = getTemplateForGeneration(authContext.companyId.value)
        val companySettings = getCompanySettings(authContext.companyId.value)
        val logoData = companySettings.basicInfo.logoId
            ?.let { logoStorageService.getLogoPath(it) }
            ?.let { Files.readAllBytes(it) }

        val generationData = InvoiceGenerationData(
            document = document,
            companySettings = CompanySettings.createCompanySettings(companySettings),
            logoData = logoData
        )

        return generatePdfFromTemplate(template, generationData)
    }

    fun generatePdfFromTemplate(
        template: ByteArray,
        generationData: InvoiceGenerationData
    ): ByteArray {
        return try {
            val renderedHtml = templateRenderingService.renderTemplate(template.let { String(it, Charsets.UTF_8) }, generationData)
            val pdfBytes = pdfGenerationService.generatePdf(renderedHtml)

            logger.debug("Successfully generated PDF, size: ${pdfBytes.size} bytes")
            pdfBytes

        } catch (e: Exception) {
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
    fun generateTemplatePreview(templateId: InvoiceTemplateId): ByteArray =
        templateGenerationService
            .generatePreview(templateId.value)

    @Transactional(readOnly = true)
    fun exportTemplate(templateId: InvoiceTemplateId, companyId: Long): ByteArray {
        logger.debug("Exporting template {} for company {}", templateId.value, companyId)

        validateCompanyId(companyId)

        val template = getTemplateForCompany(templateId, companyId)
        return template.content.htmlTemplate.toByteArray(Charsets.UTF_8)
    }

    @Transactional
    fun getTemplatesForCompany(): List<InvoiceTemplateHeaderResponse> =
        templateQueryService.getTemplatesForCurrentCompany()

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

    private fun getTemplateForGeneration(companyId: Long): ByteArray {
        return templateService
            .findActiveTemplateByTemplateType(com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType.INVOICE, companyId)
            ?.let { universalStorageService.retrieveFile(it.id) }
            ?: throw java.lang.IllegalStateException("Active template file not found for company $companyId")
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

    private fun getCompanySettings(companyId: Long): CompanySettingsResponse =
        companyDetailsFetchService.getCompanySettings(companyId)

    private fun getLogoData(companySettings: CompanySettingsResponse): ByteArray? {
        return companySettings.basicInfo.logoId?.let { logoFileId ->
            try {
                logoStorageService.getLogoPath(logoFileId)?.let { path ->
                    Files.readAllBytes(path)
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

    private fun extractHtmlContent(file: MultipartFile): String {
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
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
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

        val mockDocument = UnifiedFinancialDocument(
            id = UnifiedDocumentId("preview"),
            number = "PREVIEW/2024/001",
            type = DocumentType.INVOICE,
            title = "Przykładowa faktura - podgląd szablonu",
            description = "Podgląd szablonu faktury z przykładowymi danymi",
            issuedDate = LocalDate.now(),
            dueDate = LocalDate.now().plusDays(14),
            sellerName = companySettings.basicInfo?.companyName ?: "",
            sellerTaxId = companySettings.basicInfo?.taxId ?: "0000000000",
            sellerAddress = companySettings.basicInfo?.address ?: "ul. Przykładowa 1\n00-001 Warszawa",
            buyerName = "Przykładowy Klient Sp. z o.o.",
            buyerTaxId = "1234567890",
            buyerAddress = "ul. Kliencka 123\n00-002 Kraków",
            status = DocumentStatus.NOT_PAID,
            direction = TransactionDirection.INCOME,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            totalNet = BigDecimal("1000.00"),
            totalTax = BigDecimal("230.00"),
            totalGross = BigDecimal("1230.00"),
            paidAmount = BigDecimal.ZERO,
            currency = "PLN",
            notes = "Przykładowe uwagi do faktury",
            protocolId = null,
            protocolNumber = null,
            visitId = null,
            items = listOf(
                DocumentItem(
                    id = "item1",
                    name = "Usługa detailingowa Premium",
                    description = "Kompleksowe czyszczenie zewnętrzne i wewnętrzne pojazdu",
                    quantity = BigDecimal("1"),
                    unitPrice = BigDecimal("800.00"),
                    taxRate = BigDecimal("23"),
                    totalNet = BigDecimal("800.00"),
                    totalGross = BigDecimal("984.00")
                ),
                DocumentItem(
                    id = "item2",
                    name = "Wosk ochronny",
                    description = "Aplikacja wysokiej jakości wosku ochronnego",
                    quantity = BigDecimal("1"),
                    unitPrice = BigDecimal("200.00"),
                    taxRate = BigDecimal("23"),
                    totalNet = BigDecimal("200.00"),
                    totalGross = BigDecimal("246.00")
                )
            ),
            attachment = null,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        return InvoiceGenerationData(
            document = mockDocument,
            companySettings = CompanySettings.createCompanySettings(companySettings),
            logoData = getLogoData(companySettings),
        )
    }
}