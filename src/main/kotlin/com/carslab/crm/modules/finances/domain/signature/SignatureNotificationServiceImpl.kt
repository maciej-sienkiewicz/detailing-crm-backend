// Enhanced SignatureNotificationServiceImpl with seller signature context
package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.modules.finances.domain.signature.ports.SignatureNotificationService
import com.carslab.crm.modules.finances.domain.signature.model.SignatureCompletionData
import com.carslab.crm.signature.service.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Enhanced notification service that handles seller signature context
 */
@Service
class SignatureNotificationServiceImpl(
    private val webSocketService: WebSocketService
) : SignatureNotificationService {

    private val logger = LoggerFactory.getLogger(SignatureNotificationServiceImpl::class.java)

    override fun notifySignatureStarted(companyId: Long, sessionId: UUID, invoiceId: String) {
        logger.info("Notifying signature started for session: $sessionId")

        val notification = mapOf(
            "type" to "invoice_signature_started",
            "payload" to mapOf(
                "sessionId" to sessionId.toString(),
                "invoiceId" to invoiceId,
                "status" to "SENT_TO_TABLET",
                "timestamp" to Instant.now(),
                "includesSellerSignature" to true,
                "message" to "Invoice signature request sent with seller signature"
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }

    override fun notifySignatureCompleted(companyId: Long, sessionId: String, data: SignatureCompletionData) {
        logger.info("Notifying signature completed for session: $sessionId, hasSellerSignature: ${data.hasSellerSignature}")

        val signatureType = when {
            data.hasSellerSignature -> "fully_signed"
            else -> "client_signed"
        }

        val message = when {
            data.hasSellerSignature -> "Invoice fully signed with both client and seller signatures"
            else -> "Invoice signed with client signature only"
        }

        val notification = mapOf(
            "type" to "invoice_signature_ready",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "invoiceId" to data.invoiceId,
                "success" to data.success,
                "signatureType" to signatureType,
                "hasSellerSignature" to data.hasSellerSignature,
                "attachmentReplaced" to true,
                "signedInvoiceUrl" to "/api/invoice-signatures/sessions/$sessionId/signed-document?invoiceId=${data.invoiceId}",
                "signatureImageUrl" to "/api/invoice-signatures/sessions/$sessionId/signature-image?invoiceId=${data.invoiceId}",
                "signedAt" to data.signedAt,
                "signerName" to data.signerName,
                "timestamp" to Instant.now(),
                "message" to message,
                "newAttachment" to mapOf(
                    "id" to data.newAttachment.id,
                    "name" to data.newAttachment.name,
                    "size" to data.newAttachment.size,
                    "type" to data.newAttachment.type,
                    "uploadedAt" to data.newAttachment.uploadedAt,
                    "signatureStatus" to when {
                        data.hasSellerSignature -> "FULLY_SIGNED"
                        else -> "CLIENT_SIGNED"
                    }
                )
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }

    /**
     * NEW: Notifies about seller signature availability
     */
    fun notifySellerSignatureStatus(companyId: Long, userId: Long, hasSignature: Boolean) {
        logger.debug("Notifying seller signature status for user: $userId, hasSignature: $hasSignature")

        val notification = mapOf(
            "type" to "seller_signature_status",
            "payload" to mapOf(
                "userId" to userId,
                "hasSignature" to hasSignature,
                "timestamp" to Instant.now(),
                "message" to if (hasSignature) {
                    "Seller signature is available for invoice generation"
                } else {
                    "No seller signature found - invoices will be generated without seller signature"
                }
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }

    /**
     * NEW: Notifies about signature generation errors
     */
    fun notifySignatureError(companyId: Long, sessionId: String, error: String) {
        logger.error("Notifying signature error for session: $sessionId, error: $error")

        val notification = mapOf(
            "type" to "invoice_signature_error",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "error" to error,
                "timestamp" to Instant.now(),
                "message" to "Error occurred during signature processing"
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }
}