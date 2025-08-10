// Enhanced InvoiceAttachmentManagerImpl with seller signature support
package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.finances.domain.InvoiceAttachmentGenerationService
import com.carslab.crm.modules.finances.domain.signature.ports.InvoiceAttachmentManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class InvoiceAttachmentManagerImpl(
    private val attachmentGenerationService: InvoiceAttachmentGenerationService,
    private val storageService: UniversalStorageService,
    private val documentRepository: UnifiedDocumentRepository,
    private val securityContext: SecurityContext
) : InvoiceAttachmentManager {

    private val logger = LoggerFactory.getLogger(InvoiceAttachmentManagerImpl::class.java)

    override fun getOrGenerateUnsignedPdf(document: UnifiedFinancialDocument): ByteArray {
        logger.debug("Generating unsigned PDF for document: ${document.id.value}")
        return generateUnsignedPdfInMemory(document)
    }

    /**
     * NEW: Generates unsigned PDF with seller signature if available
     */
    override fun getOrGenerateUnsignedPdfWithSellerSignature(document: UnifiedFinancialDocument, sellerId: Long): ByteArray {
        logger.debug("Generating unsigned PDF with seller signature for document: ${document.id.value}, seller: $sellerId")
        return generateUnsignedPdfWithSellerSignatureInMemory(document, sellerId)
    }

    override fun generateSignedPdf(document: UnifiedFinancialDocument, signatureBytes: ByteArray): ByteArray {
        logger.debug("Generating signed PDF for document: ${document.id.value}")

        return attachmentGenerationService.generateSignedInvoiceAttachment(document, signatureBytes)
            ?.let { attachment ->
                storageService.retrieveFile(attachment.storageId)
            } ?: throw IllegalStateException("Failed to generate signed PDF")
    }

    /**
     * NEW: Generates fully signed PDF with both client and seller signatures
     */
    override fun generateFullySignedPdf(
        document: UnifiedFinancialDocument,
        clientSignatureBytes: ByteArray,
        sellerId: Long
    ): ByteArray {
        logger.debug("Generating fully signed PDF for document: ${document.id.value} with seller: $sellerId")

        return attachmentGenerationService.generateFullySignedInvoiceAttachment(document, clientSignatureBytes, sellerId)
            ?.let { attachment ->
                storageService.retrieveFile(attachment.storageId)
            } ?: throw IllegalStateException("Failed to generate fully signed PDF")
    }

    override fun replaceAttachment(document: UnifiedFinancialDocument, signedPdfBytes: ByteArray): DocumentAttachment {
        logger.info("Replacing attachment for document: ${document.id.value}")

        val companyId = try {
            securityContext.getCurrentCompanyId()
        } catch (e: Exception) {
            logger.warn("Failed to get company ID from security context, trying to extract from document")
            1L
        }

        try {
            val newStorageId = storeSignedPdf(signedPdfBytes, document, companyId)
            logger.info("Successfully stored new signed PDF with storage ID: $newStorageId")

            val attachmentName = determineAttachmentName(document, signedPdfBytes.size)

            val newAttachment = DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = attachmentName,
                size = signedPdfBytes.size.toLong(),
                type = "application/pdf",
                storageId = newStorageId,
                uploadedAt = LocalDateTime.now()
            )

            val oldAttachmentStorageId = document.attachment?.storageId
            logger.debug("Old attachment storage ID to cleanup: $oldAttachmentStorageId")

            val updatedDocument = document.copy(attachment = newAttachment)
            val savedDocument = documentRepository.save(updatedDocument)
            logger.info("Successfully updated document with new attachment")

            oldAttachmentStorageId?.let { oldId ->
                cleanupOldAttachment(oldId)
            } ?: logger.info("No old attachment to cleanup")

            logger.info("Successfully completed attachment replacement for document: ${document.id.value}")
            return savedDocument.attachment ?: newAttachment

        } catch (e: Exception) {
            logger.error("Failed to replace attachment for document: ${document.id.value}", e)
            throw RuntimeException("Failed to replace attachment: ${e.message}", e)
        }
    }

    private fun generateUnsignedPdfInMemory(document: UnifiedFinancialDocument): ByteArray {
        logger.debug("Generating unsigned PDF in memory for document: ${document.id.value}")

        return attachmentGenerationService.generateInvoiceAttachmentWithoutSignature(document)
            ?.let { attachment ->
                storageService.retrieveFile(attachment.storageId)?.also {
                    try {
                        storageService.deleteFile(attachment.storageId)
                        logger.debug("Cleaned up temporary unsigned PDF file")
                    } catch (e: Exception) {
                        logger.warn("Failed to cleanup temporary unsigned PDF file: ${attachment.storageId}", e)
                    }
                }
            } ?: throw IllegalStateException("Failed to generate unsigned invoice PDF")
    }

    private fun generateUnsignedPdfWithSellerSignatureInMemory(
        document: UnifiedFinancialDocument,
        sellerId: Long
    ): ByteArray {
        logger.debug("Generating unsigned PDF with seller signature in memory for document: ${document.id.value}")

        return attachmentGenerationService.generateInvoiceAttachmentWithSellerSignature(document, sellerId)
            ?.let { attachment ->
                storageService.retrieveFile(attachment.storageId)?.also {
                    try {
                        storageService.deleteFile(attachment.storageId)
                        logger.debug("Cleaned up temporary PDF file with seller signature")
                    } catch (e: Exception) {
                        logger.warn("Failed to cleanup temporary PDF file: ${attachment.storageId}", e)
                    }
                }
            } ?: run {
            logger.warn("Failed to generate PDF with seller signature, falling back to unsigned PDF")
            generateUnsignedPdfInMemory(document)
        }
    }

    private fun cleanupOldAttachment(oldStorageId: String) {
        try {
            logger.info("Attempting to delete old attachment: $oldStorageId")
            val deleted = storageService.deleteFile(oldStorageId)

            if (deleted) {
                logger.info("Successfully deleted old attachment: $oldStorageId")
            } else {
                logger.warn("Failed to delete old attachment (file may not exist): $oldStorageId")
            }
        } catch (e: Exception) {
            logger.error("Error during old attachment cleanup: $oldStorageId", e)
        }
    }

    private fun storeSignedPdf(pdfBytes: ByteArray, document: UnifiedFinancialDocument, companyId: Long): String {
        logger.debug("Storing signed PDF for document: ${document.id.value}, size: ${pdfBytes.size} bytes")

        val multipartFile = createMultipartFile(pdfBytes, document)

        return storageService.storeFile(
            UniversalStoreRequest(
                file = multipartFile,
                originalFileName = determineAttachmentName(document, pdfBytes.size),
                contentType = "application/pdf",
                companyId = companyId,
                entityId = document.id.value,
                entityType = "document",
                category = "finances",
                subCategory = "invoices/${document.direction.name.lowercase()}",
                description = "Signed invoice PDF with customer and seller signatures",
                date = document.issuedDate,
                tags = mapOf(
                    "documentType" to document.type.name,
                    "direction" to document.direction.name,
                    "signed" to "true",
                    "version" to "fully-signed",
                    "originalNumber" to document.number,
                    "hasClientSignature" to "true",
                    "hasSellerSignature" to "true"
                )
            )
        )
    }

    private fun determineAttachmentName(document: UnifiedFinancialDocument, pdfSize: Int): String {
        val sizeInKb = pdfSize / 1024
        return when {
            sizeInKb > 500 -> "fully-signed-invoice-${document.number}.pdf"
            else -> "signed-invoice-${document.number}.pdf"
        }
    }

    private fun createMultipartFile(pdfBytes: ByteArray, document: UnifiedFinancialDocument): MultipartFile {
        return object : MultipartFile {
            override fun getName(): String = "signed-invoice"
            override fun getOriginalFilename(): String = "signed-invoice-${document.number}.pdf"
            override fun getContentType(): String = "application/pdf"
            override fun isEmpty(): Boolean = pdfBytes.isEmpty()
            override fun getSize(): Long = pdfBytes.size.toLong()
            override fun getBytes(): ByteArray = pdfBytes
            override fun getInputStream(): java.io.InputStream = pdfBytes.inputStream()
            override fun transferTo(dest: java.io.File): Unit = throw UnsupportedOperationException("Transfer not supported")
        }
    }
}