package com.carslab.crm.signature.websocket

import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import com.carslab.crm.signature.api.dto.SignatureRequestDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * Extension methods for SignatureWebSocketHandler to support document signatures
 */
@Component
class DocumentSignatureWebSocketExtension(
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(DocumentSignatureWebSocketExtension::class.java)
}

/**
 * Extension methods for SignatureWebSocketHandler - Document Signatures
 */
fun SignatureWebSocketHandler.sendDocumentSignatureRequest(
    tabletId: UUID,
    request: DocumentSignatureRequestDto
): Boolean {
    val connection = getTabletConnection(tabletId)

    return if (connection != null && connection.session.isOpen && connection.authenticated) {
        val message = mapOf(
            "type" to "document_signature_request",
            "payload" to mapOf(
                "sessionId" to request.sessionId,
                "documentId" to request.documentId,
                "companyId" to request.companyId,
                "signerName" to request.signerName,
                "signatureTitle" to request.signatureTitle,
                "documentTitle" to request.documentTitle,
                "documentType" to request.documentType,
                "pageCount" to request.pageCount,
                "previewUrls" to request.previewUrls,
                "instructions" to request.instructions,
                "businessContext" to request.businessContext,
                "timeoutMinutes" to request.timeoutMinutes,
                "expiresAt" to request.expiresAt,
                "signatureFields" to request.signatureFields,
                "timestamp" to Instant.now()
            )
        )

        val success = sendToTabletSession(connection.session, message)
        if (success) {
            LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
                .info("Document signature request sent to tablet $tabletId for session ${request.sessionId}")
        } else {
            LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
                .warn("Failed to send document signature request to tablet $tabletId")
        }
        success
    } else {
        LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
            .warn("Tablet $tabletId not connected, not authenticated, or session closed")
        false
    }
}

/**
 * Extension methods for SignatureWebSocketHandler - Simple Signatures
 */

/**
 * Notify about signature completion
 */
fun SignatureWebSocketHandler.notifySignatureCompletion(sessionId: UUID, success: Boolean) {
    mapOf(
        "type" to "signature_completion_notification",
        "payload" to mapOf(
            "sessionId" to sessionId.toString(),
            "success" to success,
            "timestamp" to Instant.now()
        )
    )

    // Broadcast to relevant workstations

    LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
        .info("Signature completion notification sent for session $sessionId (success: $success)")
}

/**
 * Notify about simple signature completion
 */
fun SignatureWebSocketHandler.notifySimpleSignatureCompletion(sessionId: UUID, success: Boolean) {
    mapOf(
        "type" to "simple_signature_completion_notification",
        "payload" to mapOf(
            "sessionId" to sessionId.toString(),
            "success" to success,
            "timestamp" to Instant.now()
        )
    )

    // Broadcast to relevant workstations

    LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
        .info("Simple signature completion notification sent for session $sessionId (success: $success)")
}

/**
 * Notify about session cancellation
 */
fun SignatureWebSocketHandler.notifySessionCancellation(sessionId: UUID) {
    val message = mapOf(
        "type" to "session_cancelled",
        "payload" to mapOf(
            "sessionId" to sessionId.toString(),
            "timestamp" to Instant.now(),
            "reason" to "Session was cancelled by administrator"
        )
    )

    // Find tablet with this session and notify
    broadcastToTablets("session_cancelled", message["payload"] as Map<String, Any>)

    LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
        .info("Session cancellation notification sent for session $sessionId")
}

/**
 * Notify about simple session cancellation
 */
fun SignatureWebSocketHandler.notifySimpleSessionCancellation(sessionId: UUID) {
    val message = mapOf(
        "type" to "simple_session_cancelled",
        "payload" to mapOf(
            "sessionId" to sessionId.toString(),
            "timestamp" to Instant.now(),
            "reason" to "Simple signature session was cancelled by administrator"
        )
    )

    // Find tablet with this session and notify
    broadcastToTablets("simple_session_cancelled", message["payload"] as Map<String, Any>)

    LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
        .info("Simple session cancellation notification sent for session $sessionId")
}

/**
 * Send document viewing status update
 */
fun SignatureWebSocketHandler.sendDocumentViewingUpdate(
    tabletId: UUID,
    sessionId: UUID,
    status: String
): Boolean {
    val connection = getTabletConnection(tabletId)

    return if (connection != null && connection.session.isOpen && connection.authenticated) {
        val message = mapOf(
            "type" to "document_viewing_update",
            "payload" to mapOf(
                "sessionId" to sessionId.toString(),
                "status" to status,
                "timestamp" to Instant.now()
            )
        )

        sendToTabletSession(connection.session, message)
    } else {
        false
    }
}

/**
 * Request signature status from tablet
 */
fun SignatureWebSocketHandler.requestSignatureStatus(tabletId: UUID, sessionId: UUID): Boolean {
    return sendAdminMessage(tabletId, "signature_status_request", mapOf(
        "sessionId" to sessionId.toString(),
        "requestId" to UUID.randomUUID().toString(),
        "timestamp" to Instant.now()
    ))
}

// Helper method to get tablet connection (this would need to be added to SignatureWebSocketHandler)
private fun SignatureWebSocketHandler.getTabletConnection(tabletId: UUID): TabletConnection? {
    // This assumes you have a way to access tablet connections
    // You might need to modify SignatureWebSocketHandler to expose this
    return null // Implementation depends on your WebSocket handler structure
}

// Helper method to send message to tablet session
private fun SignatureWebSocketHandler.sendToTabletSession(
    session: org.springframework.web.socket.WebSocketSession,
    message: Map<String, Any>
): Boolean {
    return try {
        if (session.isOpen) {
            val json = ObjectMapper().writeValueAsString(message)
            session.sendMessage(org.springframework.web.socket.TextMessage(json))
            true
        } else {
            false
        }
    } catch (e: Exception) {
        LoggerFactory.getLogger(SignatureWebSocketHandler::class.java)
            .error("Error sending message to tablet session", e)
        false
    }
}

/**
 * Document signature message types for WebSocket communication
 */
object DocumentSignatureMessageTypes {
    const val DOCUMENT_SIGNATURE_REQUEST = "document_signature_request"
    const val SIMPLE_SIGNATURE_REQUEST = "simple_signature_request"
    const val DOCUMENT_VIEWING_STARTED = "document_viewing_started"
    const val DOCUMENT_VIEWING_COMPLETED = "document_viewing_completed"
    const val SIGNATURE_STARTED = "signature_started"
    const val SIGNATURE_COMPLETED = "signature_completed"
    const val SIMPLE_SIGNATURE_COMPLETED = "simple_signature_completed"
    const val SIGNATURE_CANCELLED = "signature_cancelled"
    const val SIMPLE_SESSION_CANCELLED = "simple_session_cancelled"
    const val SESSION_EXPIRED = "session_expired"
    const val DOCUMENT_PROCESSING_STATUS = "document_processing_status"
}