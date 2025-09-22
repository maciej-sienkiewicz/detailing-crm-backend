// Enhanced InvoiceSignatureOrchestrator with seller signature support
package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.domain.signature.ports.*
import com.carslab.crm.modules.finances.domain.signature.model.*
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
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
        visitId: String,
        request: InvoiceSignatureRequest,
        authContext: AuthContext
    ): InvoiceSignatureResponse {
        logger.info("Starting invoice signature process for visit: $visitId")

        return try {
            val document = documentService.createInvoiceFromVisit(visitId, request, authContext)
            requestInvoiceSignature(document.id.value, request, authContext)
        } catch (e: Exception) {
            logger.error("Failed to request invoice signature from visit: $visitId", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }

    fun requestInvoiceSignature(
        invoiceId: String,
        request: InvoiceSignatureRequest,
        authContext: AuthContext,
    ): InvoiceSignatureResponse {
        logger.info("Requesting invoice signature for invoice: $invoiceId")

        return try {
            tabletCommunicationService.validateTabletAccess(request.tabletId, authContext.companyId.value)

            val document = documentService.getDocument(invoiceId, authContext)

            val session = sessionManager.createSession(
                SignatureSessionRequest(
                    invoiceId = invoiceId,
                    tabletId = request.tabletId,
                    companyId = authContext.companyId.value,
                    signerName = request.customerName,
                    signatureTitle = request.signatureTitle,
                    instructions = request.instructions,
                    userId = authContext.userId.value,
                    timeoutMinutes = request.timeoutMinutes
                )
            )

            // Generate PDF with seller signature if available
            val unsignedPdf = attachmentManager.getOrGenerateUnsignedPdfWithSellerSignature(
                document, authContext.userId.value.toLongOrNull() ?: 1L
            )

            sessionManager.cacheDocumentForSignature(
                session.sessionId,
                document,
                unsignedPdf,
                request.customerName,
                authContext.userId.value.toLongOrNull() ?: 1L
            )

            val sent = tabletCommunicationService.sendSignatureRequest(session, document, unsignedPdf)

            if (sent) {
                sessionManager.markSessionAsSentToTablet(session.sessionId)
                notificationService.notifySignatureStarted(authContext.companyId.value, session.sessionId, invoiceId)

                InvoiceSignatureResponse(
                    success = true,
                    sessionId = session.sessionId,
                    message = "Invoice signature request sent successfully (with seller signature)",
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

            // Generate fully signed PDF with both client and seller signatures
            val signedPdf = attachmentManager.generateFullySignedPdf(
                cachedData.document,
                cachedData.signatureImageBytes,
                cachedData.sellerId
            )

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
                    newAttachment = newAttachment,
                    hasSellerSignature = true
                )
            )

            logger.info("Completed signature processed successfully for session: $sessionId (with seller signature)")
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