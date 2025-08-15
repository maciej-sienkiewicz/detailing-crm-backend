// Enhanced InvoiceAttachmentGenerationService with seller signature support
package com.carslab.crm.modules.finances.domain

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.company_settings.domain.UserSignatureApplicationService
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.PdfGenerationService
import com.carslab.crm.modules.invoice_templates.domain.ports.TemplateRenderingService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.invoice_templates.application.dto.InvoiceTemplateResponse
import com.carslab.crm.production.modules.invoice_templates.application.service.InvoiceTemplateQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import java.util.Base64

@Service
@Transactional
class InvoiceAttachmentGenerationService(
    private val logoStorageService: LogoStorageService,
    private val universalStorageService: UniversalStorageService,
    private val pdfGenerationService: PdfGenerationService,
    private val templateRenderingService: TemplateRenderingService,
    private val securityContext: SecurityContext,
    private val userSignatureApplicationService: UserSignatureApplicationService,
    private val invoiceTemplateQueryService: InvoiceTemplateQueryService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
) {
    private val logger = LoggerFactory.getLogger(InvoiceAttachmentGenerationService::class.java)

    /**
     * Generates invoice attachment without any signatures
     */
    fun generateInvoiceAttachmentWithoutSignature(document: UnifiedFinancialDocument): DocumentAttachment? {
        return generateInvoiceAttachment(document, null, null)
    }

    /**
     * Generates invoice attachment with seller signature only
     */
    fun generateInvoiceAttachmentWithSellerSignature(
        document: UnifiedFinancialDocument,
        sellerId: Long
    ): DocumentAttachment? {
        val sellerSignatureHtml = getSellerSignatureHtml(sellerId)
        return generateInvoiceAttachment(document, null, sellerSignatureHtml)
    }

    /**
     * Generates signed invoice attachment with client signature
     */
    fun generateSignedInvoiceAttachment(
        document: UnifiedFinancialDocument,
        signatureImageBytes: ByteArray
    ): DocumentAttachment? {
        val clientSignatureHtml = createClientSignatureHtml(signatureImageBytes)
        val sellerId = try {
            securityContext.getCurrentUserId()
        } catch (e: Exception) {
            logger.warn("Could not get current user ID for seller signature", e)
            null
        }

        val sellerSignatureHtml = sellerId?.let { getSellerSignatureHtml(it.toLong()) }

        return generateInvoiceAttachment(document, clientSignatureHtml, sellerSignatureHtml, "signed")
    }

    /**
     * Generates invoice attachment with both client and seller signatures
     */
    fun generateFullySignedInvoiceAttachment(
        document: UnifiedFinancialDocument,
        clientSignatureBytes: ByteArray,
        sellerId: Long
    ): DocumentAttachment? {
        val clientSignatureHtml = createClientSignatureHtml(clientSignatureBytes)
        val sellerSignatureHtml = getSellerSignatureHtml(sellerId)

        return generateInvoiceAttachment(document, clientSignatureHtml, sellerSignatureHtml, "fully-signed")
    }

    /**
     * Core method to generate invoice with optional signatures
     */
    private fun generateInvoiceAttachment(
        document: UnifiedFinancialDocument,
        clientSignatureHtml: String? = null,
        sellerSignatureHtml: String? = null,
        type: String = "unsigned"
    ): DocumentAttachment? {
        return try {
            logger.debug("Generating invoice attachment for document: ${document.id.value}, type: $type")

            val companyId = securityContext.getCurrentCompanyId()
            val activeTemplate = invoiceTemplateQueryService.findActiveTemplateForCompany()

            val companySettings = companyDetailsFetchService.getCompanySettings(companyId)

            val logoData = getLogoData()

            val additionalData = mutableMapOf<String, Any>()

            // Add client signature if provided
            if (clientSignatureHtml != null) {
                additionalData["client_signature"] = clientSignatureHtml
            } else {
                additionalData["client_signature"] = ""
            }

            // Add seller signature if provided
            if (sellerSignatureHtml != null) {
                additionalData["seller_signature"] = sellerSignatureHtml
            } else {
                additionalData["seller_signature"] = ""
            }

            val generationData = InvoiceGenerationData(
                document = document,
                companySettings = CompanySettings.createCompanySettings(companySettings),
                logoData = logoData,
                additionalData = additionalData
            )

            val pdfBytes = generatePdfFromTemplate(activeTemplate, generationData)

            logger.info("Successfully generated $type invoice attachment for document: ${document.id.value}")

            val fileName = when (type) {
                "signed" -> "signed-invoice-${document.number}.pdf"
                "fully-signed" -> "fully-signed-invoice-${document.number}.pdf"
                else -> "temp-invoice-${document.number}.pdf"
            }

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = fileName,
                size = pdfBytes.size.toLong(),
                type = "application/pdf",
                storageId = storeTempInvoicePdf(pdfBytes, document, type),
                uploadedAt = LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to generate invoice attachment for document: ${document.id.value}, type: $type", e)
            null
        }
    }

    /**
     * Retrieves seller signature HTML for given user ID (multi-tenant safe)
     */
    private fun getSellerSignatureHtml(sellerId: Long): String? {
        return try {
            val companyId = securityContext.getCurrentCompanyId()
            logger.debug("Retrieving seller signature for user: $sellerId in company: $companyId")

            val signatureResponse = userSignatureApplicationService.getUserSignature(sellerId, companyId)

            if (signatureResponse != null) {
                logger.debug("Found seller signature for user: $sellerId")

                // Extract base64 data from data URL
                val base64Data = extractBase64FromDataUrl(signatureResponse.content)
                if (base64Data != null) {
                    """<img src="data:image/png;base64,$base64Data" alt="Podpis sprzedawcy" style="max-width: 150px; max-height: 50px; object-fit: contain;"/>"""
                } else {
                    logger.warn("Could not extract base64 data from seller signature for user: $sellerId")
                    null
                }
            } else {
                logger.debug("No seller signature found for user: $sellerId in company: $companyId")
                null
            }
        } catch (e: Exception) {
            logger.error("Error retrieving seller signature for user: $sellerId", e)
            null
        }
    }

    /**
     * Creates client signature HTML from raw bytes
     */
    private fun createClientSignatureHtml(signatureImageBytes: ByteArray): String {
        val signatureBase64 = Base64.getEncoder().encodeToString(signatureImageBytes)
        return """<img src="data:image/png;base64,$signatureBase64" alt="Podpis klienta" style="max-width: 200px; max-height: 60px; object-fit: contain;"/>"""
    }

    /**
     * Extracts base64 data from data URL (e.g., "data:image/png;base64,iVBORw0KGgo...")
     */
    private fun extractBase64FromDataUrl(dataUrl: String): String? {
        return try {
            if (dataUrl.contains("base64,")) {
                dataUrl.split("base64,")[1]
            } else {
                logger.warn("Invalid data URL format: does not contain 'base64,' separator")
                null
            }
        } catch (e: Exception) {
            logger.error("Error extracting base64 from data URL", e)
            null
        }
    }

    private fun generatePdfFromTemplate(
        template: InvoiceTemplateResponse,
        data: InvoiceGenerationData
    ): ByteArray {
        logger.debug("Generating PDF from template: ${template.header.id} for document: ${data.document.id.value}")

        return try {
            val renderedHtml = templateRenderingService.renderTemplate(template, data)
            val pdfBytes = pdfGenerationService.generatePdf(renderedHtml)

            logger.debug("Successfully generated PDF, size: ${pdfBytes.size} bytes")
            pdfBytes

        } catch (e: Exception) {
            logger.error("Failed to generate PDF from template: ${template.header.id}", e)
            throw RuntimeException("PDF generation failed: ${e.message}", e)
        }
    }

    private fun storeTempInvoicePdf(
        pdfBytes: ByteArray,
        document: UnifiedFinancialDocument,
        type: String
    ): String {
        val companyId = securityContext.getCurrentCompanyId()

        val inputStreamFile = object : org.springframework.web.multipart.MultipartFile {
            override fun getName(): String = "temp-invoice"
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
                entityType = "temp-document",
                category = "finances-temp",
                subCategory = "temp-invoices/${document.direction.name.lowercase()}",
                description = "Temporary ${type.capitalize()} invoice PDF with signatures",
                date = document.issuedDate,
                tags = mapOf(
                    "documentType" to document.type.name,
                    "direction" to document.direction.name,
                    "temporary" to "true",
                    "type" to type,
                    "hasClientSignature" to (type == "signed" || type == "fully-signed").toString(),
                    "hasSellerSignature" to (type != "unsigned").toString()
                )
            )
        )
    }

    private fun getLogoData(): ByteArray? {
        return null
    }
}