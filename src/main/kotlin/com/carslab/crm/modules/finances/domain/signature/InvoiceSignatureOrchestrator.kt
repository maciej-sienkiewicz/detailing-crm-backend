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
            // 1. Find or create invoice from visit
            val document = documentService.findOrCreateInvoiceFromVisit(visitId, companyId)

            // 2. Request signature for the document
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
            // 1. Validate tablet availability
            tabletCommunicationService.validateTabletAccess(request.tabletId, companyId)

            // 2. Get document
            val document = documentService.getDocument(invoiceId)

            // 3. Create signature session
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

            // 4. Generate unsigned PDF and cache it
            val unsignedPdf = attachmentManager.getOrGenerateUnsignedPdf(document)
            sessionManager.cacheDocumentForSignature(session.sessionId, document, unsignedPdf, request.customerName)

            // 5. Send to tablet
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
            // 1. Validate session
            val session = sessionManager.getSession(sessionId)
                ?: throw InvoiceSignatureException("Session not found: $sessionId")

            // 2. Validate session is still active
            if (session.status != com.carslab.crm.signature.api.dto.SignatureSessionStatus.SENT_TO_TABLET) {
                throw InvoiceSignatureException("Session is not in valid state for signature submission: ${session.status}")
            }

            // 3. Process signature and store in cache ONLY
            val signatureBytes = extractSignatureBytes(signatureImageBase64)

            // 4. Update cache with signature data
            sessionManager.updateCacheWithSignature(sessionId, signatureImageBase64, signatureBytes)

            // 5. Mark session as completed (but don't generate final document yet)
            sessionManager.markSessionAsCompleted(sessionId)

            logger.info("Signature submission cached successfully for session: $sessionId")
            true

        } catch (e: Exception) {
            logger.error("Error processing signature submission for session: $sessionId", e)
            sessionManager.markSessionAsError(sessionId, e.message)
            false
        }
    }

    fun processCompletedSignature(sessionId: String): Boolean {
        logger.info("Processing completed signature for session: $sessionId")

        return try {
            // 1. Get cached data
            val cachedData = sessionManager.getCachedData(sessionId)
                ?: throw InvoiceSignatureException("Cached data not found for session: $sessionId")

            // 2. Validate we have signature data
            if (cachedData.signatureImageBytes.isEmpty()) {
                throw InvoiceSignatureException("No signature data found in cache for session: $sessionId")
            }

            // 3. Generate signed PDF and replace attachment
            val signedPdf = attachmentManager.generateSignedPdf(cachedData.document, cachedData.signatureImageBytes)
            val newAttachment = attachmentManager.replaceAttachment(cachedData.document, signedPdf)

            // 4. Notify frontend about completion
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

    fun getSignatureStatus(sessionId: UUID, companyId: Long, invoiceId: String): InvoiceSignatureStatusResponse {
        val status = sessionManager.getSignatureStatus(sessionId, companyId, invoiceId)

        // CRITICAL: If status is COMPLETED and we haven't processed the final document yet, do it now
        if (status.status == InvoiceSignatureStatus.COMPLETED) {
            logger.info("Detected completed signature that needs processing for session: $sessionId")

            try {
                val processed = processCompletedSignature(sessionId.toString())
                if (processed) {
                    sessionManager.cleanupCache(sessionId.toString())
                    return sessionManager.getSignatureStatus(sessionId, companyId, invoiceId)
                }
            } catch (e: Exception) {
                logger.error("Failed to process completed signature for session: $sessionId", e)
                // Continue with original status if processing fails
            }
        }

        return status
    }

    fun cancelSignatureSession(sessionId: UUID, companyId: Long, invoiceId: String, userId: String, reason: String?) {
        logger.info("Cancelling signature session: $sessionId")

        sessionManager.cancelSession(sessionId, companyId, invoiceId, userId, reason)
        tabletCommunicationService.notifySessionCancellation(sessionId)
    }

    private fun extractSignatureBytes(signatureImageBase64: String): ByteArray {
        return Base64.getDecoder().decode(signatureImageBase64.substringAfter("base64,"))
    }
}
