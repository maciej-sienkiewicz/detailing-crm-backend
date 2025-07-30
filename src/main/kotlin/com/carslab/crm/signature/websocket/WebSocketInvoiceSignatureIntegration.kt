package com.carslab.crm.signature.websocket

import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.signature.events.DocumentSignatureCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.time.Instant

fun SignatureWebSocketHandler.handleInvoiceSignatureCompleted(
    session: WebSocketSession,
    messageData: Map<String, Any>,
    eventPublisher: EventPublisher
) {
    val logger = LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
    val payload = messageData["payload"] as? Map<String, Any>
    val sessionId = payload?.get("sessionId") as? String
    val signatureImage = payload?.get("signatureImage") as? String
    val success = payload?.get("success") as? Boolean ?: false

    val tabletConnection = tabletConnections.values.find { it.session == session }

    if (tabletConnection != null && sessionId != null && tabletConnection.authenticated && tabletConnection.tablet != null) {
        logger.info("Invoice signature completed received from tablet ${tabletConnection.tablet!!.id} for session: $sessionId")

        if (success && signatureImage != null) {
            try {
                eventPublisher.publish(DocumentSignatureCompletedEvent(
                    sessionId = sessionId,
                    signatureImage = signatureImage,
                    tabletId = tabletConnection.tablet!!.id,
                    companyId = tabletConnection.companyId!!
                ))

                sendToSession(session, mapOf(
                    "type" to "invoice_signature_acknowledgment",
                    "payload" to mapOf(
                        "sessionId" to sessionId,
                        "success" to true,
                        "timestamp" to Instant.now()
                    )
                ))

                logger.info("Invoice signature event published for session: $sessionId")
            } catch (e: Exception) {
                logger.error("Failed to publish invoice signature event for session: $sessionId", e)

                sendToSession(session, mapOf(
                    "type" to "invoice_signature_acknowledgment",
                    "payload" to mapOf(
                        "sessionId" to sessionId,
                        "success" to false,
                        "error" to "Failed to process signature",
                        "timestamp" to Instant.now()
                    )
                ))
            }
        } else {
            logger.warn("Invoice signature completion without signature image or marked as failed for session: $sessionId")

            sendToSession(session, mapOf(
                "type" to "invoice_signature_acknowledgment",
                "payload" to mapOf(
                    "sessionId" to sessionId,
                    "success" to false,
                    "error" to "Missing signature or operation failed",
                    "timestamp" to Instant.now()
                )
            ))
        }
    } else {
        logger.warn("Invoice signature completed from unknown or unauthenticated tablet")
    }
}