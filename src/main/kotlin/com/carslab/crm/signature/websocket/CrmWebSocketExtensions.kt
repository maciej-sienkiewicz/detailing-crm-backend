// src/main/kotlin/com/carslab/crm/signature/websocket/CrmWebSocketExtensions.kt
package com.carslab.crm.signature.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Rozszerzenia SignatureWebSocketHandler dla komunikacji z CRM
 */

/**
 * Wyślij powiadomienie do wszystkich stacji roboczych w firmie
 */
fun SignatureWebSocketHandler.broadcastToWorkstations(
    companyId: Long,
    notification: Map<String, Any>
): Int {
    val logger = LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
    var sentCount = 0

    try {
        workstationConnections.values
            .filter { it.companyId == companyId && it.authenticated }
            .forEach { connection ->
                if (connection.session.isOpen) {
                    val success = sendToSession(connection.session, notification)
                    if (success) sentCount++
                }
            }

        logger.info("Broadcast notification sent to $sentCount workstations in company $companyId")
    } catch (e: Exception) {
        logger.error("Error broadcasting to workstations", e)
    }

    return sentCount
}

/**
 * Wyślij powiadomienie o zakończeniu podpisu protokołu do konkretnej stacji roboczej
 */
fun SignatureWebSocketHandler.notifyWorkstationAboutProtocolSignature(
    workstationId: UUID,
    protocolId: Long,
    signatureData: ProtocolSignatureCompletionData
): Boolean {
    val logger = LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)

    val connection = workstationConnections[workstationId]
    return if (connection != null && connection.session.isOpen && connection.authenticated) {
        val message = mapOf(
            "type" to "protocol_signature_completed",
            "payload" to mapOf(
                "protocolId" to protocolId,
                "sessionId" to signatureData.sessionId,
                "success" to signatureData.success,
                "signatureImageUrl" to signatureData.signatureImageUrl,
                "signedDocumentUrl" to signatureData.signedDocumentUrl,
                "signedAt" to signatureData.signedAt,
                "signerName" to signatureData.signerName,
                "timestamp" to Instant.now()
            )
        )

        val sent = sendToSession(connection.session, message)
        if (sent) {
            logger.info("Protocol signature completion notification sent to workstation $workstationId")
        } else {
            logger.warn("Failed to send notification to workstation $workstationId")
        }
        sent
    } else {
        logger.warn("Workstation $workstationId not connected or not authenticated")
        false
    }
}

/**
 * Wyślij status aktualizację podpisu protokołu
 */
fun SignatureWebSocketHandler.notifyProtocolSignatureStatus(
    companyId: Long,
    protocolId: Long,
    status: String,
    sessionId: String? = null
): Int {
    val notification = mapOf(
        "type" to "protocol_signature_status",
        "payload" to mapOf(
            "protocolId" to protocolId,
            "status" to status,
            "sessionId" to sessionId,
            "timestamp" to Instant.now()
        )
    )

    return broadcastToWorkstations(companyId, notification)
}

/**
 * Przetwórz żądanie podpisu dokumentu otrzymane z tableta
 */
fun SignatureWebSocketHandler.handleDocumentSignatureSubmission(
    session: org.springframework.web.socket.WebSocketSession,
    messageData: Map<String, Any>
) {
    val logger = LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)

    try {
        val payload = messageData["payload"] as? Map<String, Any>
        if (payload == null) {
            logger.warn("Invalid document signature submission - missing payload")
            return
        }

        val sessionId = payload["sessionId"] as? String
        val success = payload["success"] as? Boolean ?: false
        val signatureImage = payload["signatureImage"] as? String
        val signaturePlacement = payload["signaturePlacement"] as? Map<String, Any>

        if (sessionId == null) {
            logger.warn("Invalid document signature submission - missing sessionId")
            return
        }

        // Znajdź połączenie tableta
        val tabletConnection = tabletConnections.values.find { it.session == session }
        if (tabletConnection?.tablet == null) {
            logger.warn("Document signature submission from unknown tablet")
            return
        }

        logger.info("Document signature submission received from tablet ${tabletConnection.tablet.id} for session: $sessionId")

        // Tu można dodać logikę przetwarzania przez DocumentSignatureService
        // lub przekazać do odpowiedniego serwisu

        // Wyślij potwierdzenie do tableta
        sendToSession(session, mapOf(
            "type" to "document_signature_acknowledgment",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "received" to true,
                "timestamp" to Instant.now()
            )
        ))

    } catch (e: Exception) {
        logger.error("Error handling document signature submission", e)
    }
}

/**
 * Aktualizuj status przeglądania dokumentu
 */
fun SignatureWebSocketHandler.updateDocumentViewingStatus(
    sessionId: String,
    status: String,
    companyId: Long
) {
    val notification = mapOf(
        "type" to "document_viewing_status",
        "payload" to mapOf(
            "sessionId" to sessionId,
            "status" to status,
            "timestamp" to Instant.now()
        )
    )

    broadcastToWorkstations(companyId, notification)
}

// Klasy pomocnicze
data class ProtocolSignatureCompletionData(
    val sessionId: String,
    val success: Boolean,
    val signatureImageUrl: String?,
    val signedDocumentUrl: String?,
    val signedAt: Instant?,
    val signerName: String
)

/**
 * Typy wiadomości WebSocket dla komunikacji z CRM
 */
object CrmWebSocketMessageTypes {
    const val PROTOCOL_SIGNATURE_REQUEST = "protocol_signature_request"
    const val PROTOCOL_SIGNATURE_COMPLETED = "protocol_signature_completed"
    const val PROTOCOL_SIGNATURE_STATUS = "protocol_signature_status"
    const val DOCUMENT_VIEWING_STATUS = "document_viewing_status"
    const val DOCUMENT_SIGNATURE_SUBMISSION = "document_signature_submission"
    const val DOCUMENT_SIGNATURE_ACKNOWLEDGMENT = "document_signature_acknowledgment"
}