package com.carslab.crm.modules.finances.domain

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class InvoiceAttachmentGenerationService(
    private val templateRepository: InvoiceTemplateRepository,
    private val companySettingsService: CompanySettingsDomainService,
    private val logoStorageService: LogoStorageService,
    private val universalStorageService: UniversalStorageService,
    private val pdfGenerationService: PdfGenerationService,
    private val templateRenderingService: TemplateRenderingService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(InvoiceAttachmentGenerationService::class.java)

    fun generateInvoiceAttachmentWithoutSignature(document: UnifiedFinancialDocument): DocumentAttachment? {
        return try {
            logger.debug("Generating invoice attachment without signature for document: ${document.id.value}")

            val companyId = securityContext.getCurrentCompanyId()
            val activeTemplate = templateRepository.findActiveTemplateForCompany(companyId)
                ?: throw IllegalStateException("No active template found for company: $companyId")

            val companySettings = companySettingsService.getCompanySettings(companyId)
                ?: throw IllegalStateException("Company settings not found for company: $companyId")

            val logoData = getLogoData(companySettings)

            val generationData = InvoiceGenerationData(
                document = document,
                companySettings = companySettings,
                logoData = logoData,
                additionalData = mapOf("client_signature" to "")
            )

            val pdfBytes = generatePdfFromTemplate(activeTemplate, generationData)
            val storageId = storeInvoicePdf(pdfBytes, document, "unsigned")

            logger.info("Successfully generated and stored unsigned invoice attachment for document: ${document.id.value}")

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = "invoice-${document.number}.pdf",
                size = pdfBytes.size.toLong(),
                type = "application/pdf",
                storageId = storageId,
                uploadedAt = LocalDateTime.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to generate invoice attachment for document: ${document.id.value}", e)
            null
        }
    }

    fun generateSignedInvoiceAttachment(
        document: UnifiedFinancialDocument,
        signatureImageBytes: ByteArray
    ): DocumentAttachment? {
        return try {
            logger.debug("Generating signed invoice attachment for document: ${document.id.value}")

            val companyId = securityContext.getCurrentCompanyId()
            val activeTemplate = templateRepository.findActiveTemplateForCompany(companyId)
                ?: throw IllegalStateException("No active template found for company: $companyId")

            val companySettings = companySettingsService.getCompanySettings(companyId)
                ?: throw IllegalStateException("Company settings not found for company: $companyId")

            val logoData = getLogoData(companySettings)
            val signatureBase64 = java.util.Base64.getEncoder().encodeToString(signatureImageBytes)
            val signatureHtml = """<img src="data:image/png;base64,$signatureBase64" alt="Podpis klienta" style="max-width: 200px; max-height: 60px;"/>"""

            val generationData = InvoiceGenerationData(
                document = document,
                companySettings = companySettings,
                logoData = logoData,
                additionalData = mapOf("client_signature" to signatureHtml)
            )

            val signedPdfBytes = generatePdfFromTemplate(activeTemplate, generationData)
            val storageId = storeInvoicePdf(signedPdfBytes, document, "signed")

            logger.info("Successfully generated and stored signed invoice attachment for document: ${document.id.value}")

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = "signed-invoice-${document.number}.pdf",
                size = signedPdfBytes.size.toLong(),
                type = "application/pdf",
                storageId = storageId,
                uploadedAt = LocalDateTime.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to generate signed invoice attachment for document: ${document.id.value}", e)
            null
        }
    }

    private fun generatePdfFromTemplate(
        template: com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate,
        data: InvoiceGenerationData
    ): ByteArray {
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

    private fun storeInvoicePdf(
        pdfBytes: ByteArray,
        document: UnifiedFinancialDocument,
        type: String
    ): String {
        val companyId = securityContext.getCurrentCompanyId()

        val inputStreamFile = object : org.springframework.web.multipart.MultipartFile {
            override fun getName(): String = "invoice"
            override fun getOriginalFilename(): String = "$type-invoice-${document.number}.pdf"
            override fun getContentType(): String = "application/pdf"
            override fun isEmpty(): Boolean = pdfBytes.isEmpty()
            override fun getSize(): Long = pdfBytes.size.toLong()
            override fun getBytes(): ByteArray = pdfBytes
            override fun getInputStream(): java.io.InputStream = pdfBytes.inputStream()
            override fun transferTo(dest: java.io.File): Unit = throw UnsupportedOperationException("Transfer not supported")
        }

        return universalStorageService.storeFile(
            UniversalStoreRequest(
                file = inputStreamFile,
                originalFileName = "$type-invoice-${document.number}.pdf",
                contentType = "application/pdf",
                companyId = companyId,
                entityId = document.id.value,
                entityType = "document",
                category = "finances",
                subCategory = "invoices/${document.direction.name.lowercase()}",
                description = "${type.capitalize()} invoice PDF",
                date = document.issuedDate,
                tags = mapOf(
                    "documentType" to document.type.name,
                    "direction" to document.direction.name,
                    "signed" to (type == "signed").toString()
                )
            )
        )
    }

    private fun getLogoData(companySettings: com.carslab.crm.modules.company_settings.domain.model.CompanySettings): ByteArray? {
        return companySettings.logoSettings.logoFileId?.let { logoFileId ->
            try {
                logoStorageService.getLogoPath(logoFileId)?.let { path ->
                    java.nio.file.Files.readAllBytes(path)
                }
            } catch (e: Exception) {
                logger.warn("Failed to load logo for invoice generation", e)
                null
            }
        }
    }
}