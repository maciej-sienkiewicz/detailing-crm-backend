package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.domain.signature.ports.*
import com.carslab.crm.modules.finances.domain.signature.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class InvoiceSignatureOrchestrator(
    private val documentService: InvoiceDocumentService,
    private val sessionManager: SignatureSessionManager,
    private val tabletCommunicationService: TabletCommunicationService,
    private val notificationService: SignatureNotificationService,
    private val attachmentManager: InvoiceAttachmentManager
) {
    private val logger = LoggerFactory.getLogger(InvoiceSignatureOrchestrator::class.java)

    fun requestInvoiceSignatureFromVisit(
        companyId: Long,
        userId: String,
        visitId: String,
        request: InvoiceSignatureRequest
    ): InvoiceSignatureResponse {
        logger.info("Starting invoice signature process for visit: $visitId")

        return try {
            val document = documentService.findOrCreateInvoiceFromVisit(visitId, companyId, request)
            requestInvoiceSignature(companyId, userId, document.id.value, request)
        } catch (e: Exception) {
            logger.error("Failed to request invoice signature from visit: $visitId", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }

    fun requestInvoiceSignature(
        companyId: Long,
        userId: String,
        invoiceId: String,
        request: InvoiceSignatureRequest
    ): InvoiceSignatureResponse {
        logger.info("Requesting invoice signature for invoice: $invoiceId")

        return try {
            tabletCommunicationService.validateTabletAccess(request.tabletId, companyId)

            val document = documentService.getDocument(invoiceId)

            val session = sessionManager.createSession(
                SignatureSessionRequest(
                    invoiceId = invoiceId,
                    tabletId = request.tabletId,
                    companyId = companyId,
                    signerName = request.customerName,
                    signatureTitle = request.signatureTitle,
                    instructions = request.instructions,
                    userId = userId,
                    timeoutMinutes = request.timeoutMinutes
                )
            )

            val unsignedPdf = attachmentManager.getOrGenerateUnsignedPdf(document)
            sessionManager.cacheDocumentForSignature(session.sessionId, document, unsignedPdf, request.customerName)

            val sent = tabletCommunicationService.sendSignatureRequest(session, document, unsignedPdf)

            if (sent) {
                sessionManager.markSessionAsSentToTablet(session.sessionId)
                notificationService.notifySignatureStarted(companyId, session.sessionId, invoiceId)

                InvoiceSignatureResponse(
                    success = true,
                    sessionId = session.sessionId,
                    message = "Invoice signature request sent successfully",
                    invoiceId = invoiceId,
                    expiresAt = session.expiresAt
                )
            } else {
                sessionManager.markSessionAsError(session.sessionId.toString(), "Failed to send to tablet")
                throw InvoiceSignatureException("Failed to send signature request to tablet")
            }

        } catch (e: Exception) {
            logger.error("Error requesting invoice signature for invoice: $invoiceId", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }

    fun processSignatureSubmission(sessionId: String, signatureImageBase64: String): Boolean {
        logger.info("Processing signature submission for session: $sessionId")

        return try {
            val session = sessionManager.getSession(sessionId)
                ?: throw InvoiceSignatureException("Session not found: $sessionId")

            if (session.status != com.carslab.crm.signature.api.dto.SignatureSessionStatus.SENT_TO_TABLET) {
                throw InvoiceSignatureException("Session is not in valid state for signature submission: ${session.status}")
            }

            val signatureBytes = extractSignatureBytes(signatureImageBase64)
            sessionManager.updateCacheWithSignature(sessionId, signatureImageBase64, signatureBytes)
            sessionManager.markSessionAsCompleted(sessionId)

            logger.info("Signature submission cached successfully for session: $sessionId")
            true

        } catch (e: Exception) {
            logger.error("Error processing signature submission for session: $sessionId", e)
            sessionManager.markSessionAsError(sessionId, e.message)
            false
        }
    }

    fun getSignatureStatus(sessionId: UUID, companyId: Long, invoiceId: String): InvoiceSignatureStatusResponse {
        val status = sessionManager.getSignatureStatus(sessionId, companyId, invoiceId)

        if (status.status == InvoiceSignatureStatus.COMPLETED) {
            logger.info("Processing completed signature for session: $sessionId")

            try {
                val processed = processCompletedSignature(sessionId.toString())
                if (processed) {
                    sessionManager.cleanupCache(sessionId.toString())
                    return sessionManager.getSignatureStatus(sessionId, companyId, invoiceId)
                }
            } catch (e: Exception) {
                logger.error("Failed to process completed signature for session: $sessionId", e)
            }
        }

        return status
    }

    fun cancelSignatureSession(sessionId: UUID, companyId: Long, invoiceId: String, userId: String, reason: String?) {
        logger.info("Cancelling signature session: $sessionId")

        sessionManager.cancelSession(sessionId, companyId, invoiceId, userId, reason)
        tabletCommunicationService.notifySessionCancellation(sessionId)
    }

    private fun processCompletedSignature(sessionId: String): Boolean {
        return try {
            val cachedData = sessionManager.getCachedData(sessionId)
                ?: throw InvoiceSignatureException("Cached data not found for session: $sessionId")

            if (cachedData.signatureImageBytes.isEmpty()) {
                throw InvoiceSignatureException("No signature data found in cache for session: $sessionId")
            }

            val signedPdf = attachmentManager.generateSignedPdf(cachedData.document, cachedData.signatureImageBytes)
            val newAttachment = attachmentManager.replaceAttachment(cachedData.document, signedPdf)

            notificationService.notifySignatureCompleted(
                cachedData.companyId,
                sessionId,
                SignatureCompletionData(
                    sessionId = sessionId,
                    invoiceId = cachedData.document.id.value,
                    success = true,
                    signedAt = java.time.Instant.now(),
                    signerName = cachedData.signerName,
                    newAttachment = newAttachment
                )
            )

            logger.info("Completed signature processed successfully for session: $sessionId")
            true

        } catch (e: Exception) {
            logger.error("Error processing completed signature for session: $sessionId", e)
            sessionManager.markSessionAsError(sessionId, e.message)
            false
        }
    }

    private fun extractSignatureBytes(signatureImageBase64: String): ByteArray {
        return Base64.getDecoder().decode(signatureImageBase64.substringAfter("base64,"))
    }
}