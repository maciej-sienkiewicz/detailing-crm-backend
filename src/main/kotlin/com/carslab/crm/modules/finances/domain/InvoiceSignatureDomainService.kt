package com.carslab.crm.modules.finances.domain

import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.infrastructure.service.CachedInvoiceSignatureData
import com.carslab.crm.modules.finances.infrastructure.service.InvoiceSignatureCacheService
import com.carslab.crm.modules.finances.infrastructure.service.toInvoiceSignatureData
import com.carslab.crm.signature.service.CachedSignatureData
import com.carslab.crm.signature.service.WebSocketService
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentSignatureSessionRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.infrastructure.persistance.entity.DocumentSignatureSession
import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class InvoiceSignatureDomainService(
    private val invoiceSignatureCacheService: InvoiceSignatureCacheService,
    private val invoiceAttachmentGenerationService: InvoiceAttachmentGenerationService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val webSocketService: WebSocketService,
    private val universalStorageService: UniversalStorageService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(InvoiceSignatureDomainService::class.java)

    fun cacheInvoiceForSignature(
        document: UnifiedFinancialDocument,
        sessionId: String,
        tabletId: String,
        companyId: Long,
        signerName: String,
        metadata: Map<String, Any>
    ): ByteArray {
        logger.info("Caching invoice for signature: ${document.id.value}")

        val invoicePdfBytes = getOrGenerateUnsignedInvoicePdf(document)

        val cachedData = CachedInvoiceSignatureData(
            sessionId = sessionId,
            invoiceId = document.id.value,
            signatureImageBase64 = "",
            signatureImageBytes = ByteArray(0),
            originalInvoiceBytes = invoicePdfBytes,
            signedAt = Instant.now(),
            signerName = signerName,
            tabletId = tabletId,
            companyId = companyId,
            metadata = metadata
        )

        invoiceSignatureCacheService.cacheInvoiceSignature(sessionId, cachedData)

        return invoicePdfBytes
    }

    fun createSignatureSession(
        invoiceId: String,
        tabletId: UUID,
        companyId: Long,
        signerName: String,
        signatureTitle: String,
        instructions: String?,
        userId: String,
        timeoutMinutes: Int
    ): DocumentSignatureSession {
        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        val session = DocumentSignatureSession(
            sessionId = sessionId,
            documentId = UUID.randomUUID(),
            tabletId = tabletId,
            companyId = companyId,
            signerName = signerName,
            signatureTitle = signatureTitle,
            instructions = instructions,
            businessContext = objectMapper.writeValueAsString(
                mapOf(
                    "invoiceId" to invoiceId,
                    "documentType" to "INVOICE",
                    "replaceOriginalAttachment" to true
                )
            ),
            createdBy = userId,
            expiresAt = expiresAt
        )

        return documentSignatureSessionRepository.save(session)
    }

    fun sendDocumentSignatureRequest(
        session: DocumentSignatureSession,
        document: UnifiedFinancialDocument,
        invoicePdfBytes: ByteArray
    ): Boolean {
        val documentRequest = DocumentSignatureRequestDto(
            sessionId = session.sessionId.toString(),
            documentId = session.documentId.toString(),
            companyId = session.companyId,
            signerName = session.signerName,
            signatureTitle = session.signatureTitle,
            documentTitle = "Faktura ${document.number}",
            documentType = "INVOICE",
            pageCount = 1,
            previewUrls = emptyList(),
            instructions = session.instructions,
            businessContext = mapOf(
                "invoiceId" to document.id.value,
                "documentType" to "INVOICE"
            ),
            timeoutMinutes = ChronoUnit.MINUTES.between(Instant.now(), session.expiresAt).toInt(),
            expiresAt = session.expiresAt.toString(),
            signatureFields = null
        )

        return webSocketService.sendDocumentSignatureRequestWithDocument(
            session.tabletId,
            documentRequest,
            invoicePdfBytes
        )
    }

    fun processSignatureFromTabletNotReplace(sessionId: String, signatureImageBase64: String): Boolean {
        logger.info("Processing invoice signature from tablet for session: $sessionId")

        try {
            val signatureBytes = Base64.getDecoder().decode(
                signatureImageBase64.substringAfter("base64,")
            )

            val updatedData = invoiceSignatureCacheService.updateInvoiceSignature(sessionId) { cachedData ->
                cachedData.copy(
                    signatureImageBase64 = signatureImageBase64,
                    signatureImageBytes = signatureBytes,
                    signedAt = Instant.now()
                )
            }

            if (updatedData == null) {
                logger.error("Failed to update invoice signature cache - session not found: $sessionId")
                return false
            }

            val session = documentSignatureSessionRepository.findBySessionId(UUID.fromString(sessionId))
            if (session != null) {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.COMPLETED)
                )
            }
            
            notifyFrontendSignatureComplete(updatedData.companyId, sessionId, updatedData)

            logger.info("Invoice signature processed and frontend notified for session: $sessionId")
            return true

        } catch (e: Exception) {
            logger.error("Error processing invoice signature from tablet", e)
            return false
        }
    }


    fun processSignatureFromTablet(sessionId: String, signatureImageBase64: String): Boolean {
        logger.info("Processing invoice signature from tablet for session: $sessionId")

        try {
            val signatureBytes = Base64.getDecoder().decode(
                signatureImageBase64.substringAfter("base64,")
            )

            val updatedData = invoiceSignatureCacheService.updateInvoiceSignature(sessionId) { cachedData ->
                cachedData.copy(
                    signatureImageBase64 = signatureImageBase64,
                    signatureImageBytes = signatureBytes,
                    signedAt = Instant.now()
                )
            }

            if (updatedData == null) {
                logger.error("Failed to update invoice signature cache - session not found: $sessionId")
                return false
            }

            val session = documentSignatureSessionRepository.findBySessionId(UUID.fromString(sessionId))
            if (session != null) {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.COMPLETED)
                )
            }

            val shouldReplaceAttachment = updatedData.metadata["replaceOriginalAttachment"] as? Boolean ?: false
            if (shouldReplaceAttachment) {
                replaceOriginalAttachmentWithSignedVersion(updatedData)
            }

            notifyFrontendSignatureComplete(updatedData.companyId, sessionId, updatedData)

            logger.info("Invoice signature processed and frontend notified for session: $sessionId")
            return true

        } catch (e: Exception) {
            logger.error("Error processing invoice signature from tablet", e)
            return false
        }
    }

    fun getInvoiceSignatureStatus(
        sessionId: UUID,
        companyId: Long,
        invoiceId: String
    ): InvoiceSignatureStatusResponse {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw IllegalArgumentException("Signature session not found")

        val businessContext = session.businessContext?.let {
            objectMapper.readValue(it, Map::class.java)
        }
        val contextInvoiceId = businessContext?.get("invoiceId") as? String

        if (contextInvoiceId != invoiceId) {
            throw IllegalArgumentException("Session does not belong to this invoice")
        }

        val currentStatus = if (session.isExpired() && session.status in listOf(
                SignatureSessionStatus.PENDING,
                SignatureSessionStatus.SENT_TO_TABLET,
                SignatureSessionStatus.VIEWING_DOCUMENT,
                SignatureSessionStatus.SIGNING_IN_PROGRESS
            )
        ) {
            documentSignatureSessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
            SignatureSessionStatus.EXPIRED
        } else {
            session.status
        }

        return InvoiceSignatureStatusResponse(
            success = true,
            sessionId = sessionId,
            invoiceId = invoiceId,
            status = mapToInvoiceStatus(currentStatus),
            signedAt = session.signedAt,
            signedInvoiceUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoice-signatures/sessions/$sessionId/signed-document?invoiceId=$invoiceId" else null,
            signatureImageUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoice-signatures/sessions/$sessionId/signature-image?invoiceId=$invoiceId" else null,
            timestamp = Instant.now()
        )
    }

    fun getSignedInvoice(sessionId: UUID, companyId: Long, invoiceId: String): ByteArray? {
        val cachedData = invoiceSignatureCacheService.getInvoiceSignature(sessionId.toString())

        if (cachedData == null || cachedData.companyId != companyId) {
            logger.warn("Signed invoice not found or access denied for session: $sessionId")
            return null
        }

        if (cachedData.invoiceId != invoiceId) {
            logger.warn("Session does not belong to invoice: $invoiceId")
            return null
        }

        if (cachedData.signatureImageBytes.isEmpty()) {
            logger.warn("Signature not yet available for session: $sessionId")
            return null
        }

        return cachedData.originalInvoiceBytes
    }

    fun getCachedSignatureData(sessionId: String): CachedSignatureData? {
        return invoiceSignatureCacheService.getInvoiceSignature(sessionId)?.let { invoiceData ->
            CachedSignatureData(
                sessionId = invoiceData.sessionId,
                signatureImageBase64 = invoiceData.signatureImageBase64,
                signatureImageBytes = invoiceData.signatureImageBytes,
                originalDocumentBytes = invoiceData.originalInvoiceBytes,
                signedAt = invoiceData.signedAt,
                signerName = invoiceData.signerName,
                protocolId = null,
                tabletId = UUID.fromString(invoiceData.tabletId),
                companyId = invoiceData.companyId,
                metadata = invoiceData.metadata
            )
        }
    }

    private fun getOrGenerateUnsignedInvoicePdf(document: UnifiedFinancialDocument): ByteArray {
        return document.attachment?.let { attachment ->
            try {
                universalStorageService.retrieveFile(attachment.storageId)
            } catch (e: Exception) {
                logger.warn("Failed to retrieve existing attachment, generating new one", e)
                null
            }
        } ?: run {
            val unsignedAttachment =
                invoiceAttachmentGenerationService.generateInvoiceAttachmentWithoutSignature(document)
                    ?: throw IllegalStateException("Failed to generate unsigned invoice PDF")
            universalStorageService.retrieveFile(unsignedAttachment.storageId)
                ?: throw IllegalStateException("Failed to retrieve generated invoice PDF")
        }
    }

    private fun replaceOriginalAttachmentWithSignedVersion(cachedData: CachedInvoiceSignatureData) {
        try {
            logger.info("Replacing original attachment with signed version for invoice: ${cachedData.invoiceId}")

            val signedAttachment = invoiceAttachmentGenerationService.generateSignedInvoiceAttachment(
                document = getDocumentFromCache(cachedData),
                signatureImageBytes = cachedData.signatureImageBytes
            )

            if (signedAttachment != null) {
                logger.info("Successfully replaced attachment with signed version for invoice: ${cachedData.invoiceId}")
            } else {
                logger.error("Failed to generate signed attachment for invoice: ${cachedData.invoiceId}")
            }

        } catch (e: Exception) {
            logger.error("Failed to replace original attachment with signed version", e)
        }
    }

    private fun getDocumentFromCache(cachedData: CachedInvoiceSignatureData): UnifiedFinancialDocument {
        return cachedData.metadata["document"] as? UnifiedFinancialDocument
            ?: throw IllegalStateException("Document not found in cache metadata")
    }

    fun notifyFrontendSignatureComplete(
        companyId: Long,
        sessionId: String,
        cachedData: CachedInvoiceSignatureData
    ) {
        val notification = mapOf(
            "type" to "invoice_signature_ready",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "invoiceId" to cachedData.invoiceId,
                "success" to true,
                "attachmentReplaced" to (cachedData.metadata["replaceOriginalAttachment"] as? Boolean ?: false),
                "signedInvoiceUrl" to "/api/invoice-signatures/sessions/$sessionId/signed-document?invoiceId=${cachedData.invoiceId}",
                "signatureImageUrl" to "/api/invoice-signatures/sessions/$sessionId/signature-image?invoiceId=${cachedData.invoiceId}",
                "signedAt" to cachedData.signedAt,
                "signerName" to cachedData.signerName,
                "timestamp" to Instant.now()
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }

    private fun mapToInvoiceStatus(status: SignatureSessionStatus): InvoiceSignatureStatus {
        return when (status) {
            SignatureSessionStatus.PENDING -> InvoiceSignatureStatus.PENDING
            SignatureSessionStatus.SENT_TO_TABLET -> InvoiceSignatureStatus.SENT_TO_TABLET
            SignatureSessionStatus.VIEWING_DOCUMENT -> InvoiceSignatureStatus.VIEWING_INVOICE
            SignatureSessionStatus.SIGNING_IN_PROGRESS -> InvoiceSignatureStatus.SIGNING_IN_PROGRESS
            SignatureSessionStatus.COMPLETED -> InvoiceSignatureStatus.COMPLETED
            SignatureSessionStatus.EXPIRED -> InvoiceSignatureStatus.EXPIRED
            SignatureSessionStatus.CANCELLED -> InvoiceSignatureStatus.CANCELLED
            SignatureSessionStatus.ERROR -> InvoiceSignatureStatus.ERROR
        }
    }
}