package com.carslab.crm.modules.invoice_templates.domain

import com.carslab.crm.modules.invoice_templates.domain.model.*
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.invoice_templates.infrastructure.templates.ProfessionalDefaultTemplateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class InvoiceTemplateService(
    private val templateRepository: InvoiceTemplateRepository,
    private val pdfGenerationService: PdfGenerationService,
    private val templateRenderingService: TemplateRenderingService,
    private val companySettingsService: CompanySettingsDomainService,
    private val logoStorageService: LogoStorageService,
    private val professionalDefaultTemplateProvider: ProfessionalDefaultTemplateProvider,
) {
    private val logger = LoggerFactory.getLogger(InvoiceTemplateService::class.java)

    fun generateInvoicePdf(
        companyId: Long,
        invoiceData: InvoiceGenerationData,
        templateId: InvoiceTemplateId? = null
    ): ByteArray {
        val template = templateId?.let {
            templateRepository.findById(it)
        } ?: templateRepository.findActiveTemplateForCompany(companyId)
        ?: getSystemDefaultTemplate()

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        val companySettings = companySettingsService.getCompanySettings(companyId)
            ?: throw IllegalStateException("Company settings not found for company: $companyId")

        val logoData = companySettings.logoSettings.logoFileId?.let { logoFileId ->
            try {
                logoStorageService.getLogoPath(logoFileId)?.let { path ->
                    java.nio.file.Files.readAllBytes(path)
                }
            } catch (e: Exception) {
                logger.warn("Failed to load logo for company: {}", companyId, e)
                null
            }
        }

        val generationData = invoiceData.copy(
            companySettings = companySettings,
            logoData = logoData
        )

        val renderedHtml = templateRenderingService.renderTemplate(template, generationData)
        return pdfGenerationService.generatePdf(renderedHtml, template.content.layout)
    }

    fun uploadTemplate(
        file: MultipartFile,
        companyId: Long,
        name: String,
        description: String?
    ): InvoiceTemplate {
        if (file.isEmpty) {
            throw IllegalArgumentException("File cannot be empty")
        }

        if (file.contentType != "text/html" && !file.originalFilename?.endsWith(".html", ignoreCase = true)!!) {
            throw IllegalArgumentException("File must be HTML format")
        }

        if (file.size > 1024 * 1024) {
            throw IllegalArgumentException("File size cannot exceed 1MB")
        }

        val htmlContent = try {
            String(file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to read HTML content from file", e)
        }

        templateRepository.deactivateAllTemplatesForCompany(companyId)

        val template = InvoiceTemplate(
            id = InvoiceTemplateId.generate(),
            companyId = companyId,
            name = name,
            description = description,
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
            isActive = true,
            metadata = TemplateMetadata(
                version = "1.0",
                author = null,
                tags = setOf("custom", "uploaded"),
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

        return templateRepository.save(template)
    }

    fun activateTemplate(companyId: Long, templateId: InvoiceTemplateId) {
        val template = templateRepository.findById(templateId)
            ?: throw IllegalArgumentException("Template not found: ${templateId.value}")

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        templateRepository.deactivateAllTemplatesForCompany(companyId)
        val activatedTemplate = template.copy(isActive = true)
        templateRepository.save(activatedTemplate)
    }

    fun generateTemplatePreview(templateId: InvoiceTemplateId, companyId: Long): ByteArray {
        val template = templateRepository.findById(templateId)
            ?: throw IllegalArgumentException("Template not found: ${templateId.value}")

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        val mockData = createMockInvoiceData(companyId)
        val renderedHtml = templateRenderingService.renderTemplate(template, mockData)
        return pdfGenerationService.generatePdf(renderedHtml, template.content.layout)
    }

    fun exportTemplate(templateId: InvoiceTemplateId, companyId: Long): ByteArray {
        val template = templateRepository.findById(templateId)
            ?: throw IllegalArgumentException("Template not found: ${templateId.value}")

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        return template.content.htmlTemplate.toByteArray(Charsets.UTF_8)
    }

    @Transactional
    fun getTemplatesForCompany(companyId: Long): List<InvoiceTemplate> {
        val existingTemplates = templateRepository.findByCompanyId(companyId)
            .filter { it.companyId == companyId }

        return if (existingTemplates.isEmpty()) {
            val defaultTemplate = professionalDefaultTemplateProvider.createDefaultTemplate(companyId)
            val savedTemplate = templateRepository.save(defaultTemplate)
            listOf(savedTemplate)
        } else {
            existingTemplates
        }
    }

    @Transactional(readOnly = true)
    fun getTemplate(templateId: InvoiceTemplateId, companyId: Long): InvoiceTemplate {
        val template = templateRepository.findById(templateId)
            ?: throw IllegalArgumentException("Template not found: ${templateId.value}")

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        return template
    }

    fun deleteTemplate(templateId: InvoiceTemplateId, companyId: Long): Boolean {
        val template = templateRepository.findById(templateId) ?: return false

        if (!template.canBeUsedBy(companyId)) {
            throw IllegalArgumentException("Template cannot be used by company: $companyId")
        }

        if (template.templateType == TemplateType.SYSTEM_DEFAULT) {
            throw IllegalArgumentException("Cannot delete system default template")
        }

        return templateRepository.deleteById(templateId)
    }

    private fun getSystemDefaultTemplate(): InvoiceTemplate {
        return templateRepository.findSystemDefaultTemplate()
            ?: throw IllegalStateException("System default template not found")
    }

    private fun extractCssFromHtml(html: String): String {
        val stylePattern = """<style[^>]*>(.*?)</style>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = stylePattern.findAll(html)
        return matches.joinToString("\n") { it.groupValues[1] }
    }

    private fun createMockInvoiceData(companyId: Long): InvoiceGenerationData {
        val companySettings = companySettingsService.getCompanySettings(companyId)
            ?: throw IllegalStateException("Company settings not found")

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
            notes = "Przykładowe uwagi do faktury - to jest szablon do podglądu",
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
            logoData = null
        )
    }
}