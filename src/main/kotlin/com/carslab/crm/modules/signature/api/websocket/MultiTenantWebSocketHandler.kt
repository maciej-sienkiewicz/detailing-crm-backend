package com.carslab.crm.signature.api.websocket

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.exception.UnauthorizedTabletException
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.service.TabletManagementService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class TabletConnection(
    val session: WebSocketSession,
    val tablet: TabletDevice,
    val locationId: UUID
)

data class WorkstationConnection(
    val session: WebSocketSession,
    val workstationId: UUID,
    val companyId: Long
)

@Component
class MultiTenantWebSocketHandler(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val tabletManagementService: TabletManagementService,
    private val objectMapper: ObjectMapper,
    private val securityContext: SecurityContext
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val tabletConnections = ConcurrentHashMap<UUID, TabletConnection>()
    private val workstationConnections = ConcurrentHashMap<UUID, WorkstationConnection>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val uri = session.uri.toString()

            when {
                uri.contains("/ws/tablet/") -> handleTabletConnection(session)
                uri.contains("/ws/workstation/") -> handleWorkstationConnection(session)
                else -> {
                    logger.warn("Unknown WebSocket connection type: $uri")
                    session.close()
                }
            }
        } catch (e: Exception) {
            logger.error("Error establishing WebSocket connection", e)
            session.close()
        }
    }

    private fun handleTabletConnection(session: WebSocketSession) {
        val deviceId = extractDeviceId(session.uri.toString())
        val token = session.handshakeHeaders.getFirst("X-Device-Token")

        if (deviceId == null || token == null) {
            throw UnauthorizedTabletException("Missing required headers")
        }

        val tablet = tabletDeviceRepository.findByIdAndDeviceToken(deviceId, token)
            ?: throw UnauthorizedTabletException("Invalid device credentials")

        val connection = TabletConnection(
            session = session,
            tablet = tablet,
            locationId = tablet.locationId
        )

        tabletConnections[deviceId] = connection
        tabletManagementService.updateTabletLastSeen(deviceId)

        logger.info("Tablet connected: ${tablet.friendlyName} (${deviceId})")

        // Send connection confirmation
        sendToSession(session, ConnectionStatusMessage("connected"))
    }

    private fun handleWorkstationConnection(session: WebSocketSession) {
        val workstationId = extractWorkstationId(session.uri.toString())
        val companyId = securityContext.getCurrentCompanyId()

        if (workstationId == null || companyId == null) {
            session.close(CloseStatus.BAD_DATA)
            return
        }

        val connection = WorkstationConnection(
            session = session,
            workstationId = workstationId,
            companyId = companyId
        )

        workstationConnections[workstationId] = connection

        logger.info("Workstation connected: $workstationId")

        // Send connection confirmation
        sendToSession(session, ConnectionStatusMessage("connected"))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val messageData = objectMapper.readValue(message.payload, Map::class.java) as Map<String, Any>
            val messageType = messageData["type"] as? String

            when (messageType) {
                "heartbeat" -> handleHeartbeat(session)
                "signature_completed" -> {
                    @Suppress("UNCHECKED_CAST")
                    handleSignatureCompleted(session, messageData as Map<String, Any>)
                }
                else -> logger.warn("Unknown message type: $messageType")
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message", e)
            sendToSession(session, ErrorMessage("Invalid message format"))
        }
    }


    private fun handleHeartbeat(session: WebSocketSession) {
        // Find tablet by session and update last seen
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletManagementService.updateTabletLastSeen(connection.tablet.id)
        }
    }

    private fun handleSignatureCompleted(session: WebSocketSession, messageData: Map<String, Any>) {
        // Handle signature completion acknowledgment from tablet
        val sessionId = messageData["sessionId"] as? String
        logger.info("Signature completed acknowledgment received for session: $sessionId")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // Remove tablet connection
        tabletConnections.values.removeIf { it.session == session }

        // Remove workstation connection
        workstationConnections.values.removeIf { it.session == session }

        logger.info("WebSocket connection closed: ${status.reason}")
    }

    fun isTabletConnected(tabletId: UUID): Boolean {
        val connection = tabletConnections[tabletId]
        return connection?.session?.isOpen == true
    }

    private fun sendToSession(session: WebSocketSession, message: WebSocketMessage) {
        try {
            if (session.isOpen) {
                val json = objectMapper.writeValueAsString(mapOf(
                    "type" to message.type,
                    "payload" to message
                ))
                session.sendMessage(TextMessage(json))
            }
        } catch (e: Exception) {
            logger.error("Error sending WebSocket message", e)
        }
    }

    private fun extractDeviceId(uri: String): UUID? {
        return try {
            val parts = uri.split("/")
            val deviceIdIndex = parts.indexOf("tablet") + 1
            if (deviceIdIndex < parts.size) {
                UUID.fromString(parts[deviceIdIndex].split("?")[0])
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractWorkstationId(uri: String): UUID? {
        return try {
            val parts = uri.split("/")
            val workstationIdIndex = parts.indexOf("workstation") + 1
            if (workstationIdIndex < parts.size) {
                UUID.fromString(parts[workstationIdIndex].split("?")[0])
            } else null
        } catch (e: Exception) {
            null
        }
    }
}