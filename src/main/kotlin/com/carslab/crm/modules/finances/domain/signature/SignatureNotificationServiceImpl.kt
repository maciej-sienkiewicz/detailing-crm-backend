package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.modules.finances.domain.signature.ports.SignatureNotificationService
import com.carslab.crm.modules.finances.domain.signature.model.SignatureCompletionData
import com.carslab.crm.signature.service.WebSocketService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Handles notifications to frontend applications
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
                "timestamp" to Instant.now()
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }

    override fun notifySignatureCompleted(companyId: Long, sessionId: String, data: SignatureCompletionData) {
        logger.info("Notifying signature completed for session: $sessionId")

        val notification = mapOf(
            "type" to "invoice_signature_ready",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "invoiceId" to data.invoiceId,
                "success" to data.success,
                "attachmentReplaced" to true,
                "signedInvoiceUrl" to "/api/invoice-signatures/sessions/$sessionId/signed-document?invoiceId=${data.invoiceId}",
                "signatureImageUrl" to "/api/invoice-signatures/sessions/$sessionId/signature-image?invoiceId=${data.invoiceId}",
                "signedAt" to data.signedAt,
                "signerName" to data.signerName,
                "timestamp" to Instant.now(),
                "newAttachment" to mapOf(
                    "id" to data.newAttachment.id,
                    "name" to data.newAttachment.name,
                    "size" to data.newAttachment.size,
                    "type" to data.newAttachment.type,
                    "uploadedAt" to data.newAttachment.uploadedAt
                )
            )
        )

        webSocketService.broadcastToWorkstations(companyId, notification)
    }
}
