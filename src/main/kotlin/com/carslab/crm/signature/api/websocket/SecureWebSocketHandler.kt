package com.carslab.crm.signature.websocket

import com.carslab.crm.audit.service.AuditService
import com.carslab.crm.security.JwtService
import com.carslab.crm.signature.domain.service.TabletManagementService
import com.carslab.crm.signature.entity.DeviceStatus
import com.carslab.crm.signature.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.repository.TabletDeviceRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class TabletConnection(
    val session: WebSocketSession,
    val tablet: TabletDevice,
    val tenantId: UUID,
    val locationId: UUID,
    val connectedAt: Instant,
    val lastHeartbeat: Instant
)

data class WorkstationConnection(
    val session: WebSocketSession,
    val workstationId: UUID,
    val tenantId: UUID,
    val userId: UUID,
    val connectedAt: Instant,
    val lastHeartbeat: Instant
)

@Component
class SecureWebSocketHandler(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val tabletManagementService: TabletManagementService,
    private val objectMapper: ObjectMapper,
    private val auditService: AuditService,
    private val jwtService: JwtService
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val tabletConnections = ConcurrentHashMap<UUID, TabletConnection>()
    private val workstationConnections = ConcurrentHashMap<UUID, WorkstationConnection>()
    private val heartbeatExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    init {
        heartbeatExecutor.scheduleAtFixedRate(::cleanupStaleConnections, 30, 30, TimeUnit.SECONDS)
        heartbeatExecutor.scheduleAtFixedRate(::sendHeartbeats, 25, 25, TimeUnit.SECONDS)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val uri = session.uri.toString()

            when {
                uri.contains("/ws/tablet/") -> handleTabletConnection(session)
                uri.contains("/ws/workstation/") -> handleWorkstationConnection(session)
                else -> {
                    logger.warn("Unknown WebSocket connection type: $uri")
                    session.close(CloseStatus.NOT_ACCEPTABLE)
                }
            }
        } catch (e: SecurityException) {
            logger.error("Security error establishing WebSocket connection", e)
            auditService.logSecurityViolation("websocket_unauthorized", session.remoteAddress?.toString(), e.message)
            session.close(CloseStatus.NOT_ACCEPTABLE)
        } catch (e: Exception) {
            logger.error("Error establishing WebSocket connection", e)
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    private fun handleTabletConnection(session: WebSocketSession) {
        val deviceId = extractDeviceId(session.uri.toString())
            ?: throw SecurityException("Missing device ID")

        val token = session.handshakeHeaders.getFirst("X-Device-Token")
            ?: throw SecurityException("Missing device token")

        val tenantId = session.handshakeHeaders.getFirst("X-Tenant-Id")?.let { UUID.fromString(it) }
            ?: throw SecurityException("Missing tenant ID")

        val tablet = tabletDeviceRepository.findByIdAndDeviceToken(deviceId, token)
            ?: throw SecurityException("Invalid device credentials")

        if (tablet.tenantId != tenantId) {
            throw SecurityException("Tenant mismatch")
        }

        if (tablet.status != DeviceStatus.ACTIVE) {
            throw SecurityException("Device not active")
        }

        tabletConnections[deviceId]?.let { existingConnection ->
            if (existingConnection.session.isOpen) {
                logger.warn("Closing existing connection for tablet: $deviceId")
                existingConnection.session.close(CloseStatus.NORMAL.withReason("New connection established"))
            }
        }

        val connection = TabletConnection(
            session = session,
            tablet = tablet,
            tenantId = tablet.tenantId,
            locationId = tablet.locationId,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now()
        )

        tabletConnections[deviceId] = connection
        tabletManagementService.updateTabletLastSeen(deviceId)

        logger.info("Tablet connected: ${tablet.friendlyName} (${deviceId}) from ${session.remoteAddress}")
        auditService.logTabletConnection(deviceId, tablet.tenantId, "CONNECTED")

        sendToSession(session, ConnectionStatusMessage("connected"))
    }

    private fun handleWorkstationConnection(session: WebSocketSession) {
        val workstationId = extractWorkstationId(session.uri.toString())
            ?: throw SecurityException("Missing workstation ID")

        val authHeader = session.handshakeHeaders.getFirst("Authorization")
            ?: throw SecurityException("Missing authorization header")

        val token = if (authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            throw SecurityException("Invalid authorization format")
        }

        if (!jwtService.validateToken(token)) {
            throw SecurityException("Invalid JWT token")
        }

        val claims = jwtService.extractClaims(token)

        if (!claims.permissions.contains("workstation:connect")) {
            throw SecurityException("Insufficient permissions")
        }

        val connection = WorkstationConnection(
            session = session,
            workstationId = workstationId,
            tenantId = claims.tenantId,
            userId = claims.userId,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now()
        )

        workstationConnections[workstationId] = connection

        logger.info("Workstation connected: $workstationId by user ${claims.userId}")
        auditService.logWorkstationConnection(workstationId, claims.tenantId, claims.userId, "CONNECTED")

        sendToSession(session, ConnectionStatusMessage("connected"))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val messageData = objectMapper.readValue(message.payload, Map::class.java)
            val messageType = messageData["type"] as? String

            when (messageType) {
                "heartbeat" -> handleHeartbeat(session)
                "signature_completed" -> handleSignatureCompleted(session, messageData)
                "tablet_status" -> handleTabletStatusUpdate(session, messageData)
                else -> {
                    logger.warn("Unknown message type: $messageType from ${session.remoteAddress}")
                    sendToSession(session, ErrorMessage("Unknown message type"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message from ${session.remoteAddress}", e)
            sendToSession(session, ErrorMessage("Invalid message format"))
        }
    }

    private fun handleHeartbeat(session: WebSocketSession) {
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections[connection.tablet.id] = connection.copy(lastHeartbeat = Instant.now())
            tabletManagementService.updateTabletLastSeen(connection.tablet.id)
        }

        workstationConnections.values.find { it.session == session }?.let { connection ->
            workstationConnections[connection.workstationId] = connection.copy(lastHeartbeat = Instant.now())
        }

        sendToSession(session, HeartbeatMessage())
    }

    private fun handleSignatureCompleted(session: WebSocketSession, messageData: Map<String, Any>) {
        val sessionId = messageData["sessionId"] as? String
        val success = messageData["success"] as? Boolean ?: false

        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && sessionId != null) {
            logger.info("Signature completion acknowledgment received from tablet ${tabletConnection.tablet.id} for session: $sessionId")
            auditService.logSignatureAcknowledgment(tabletConnection.tablet.id, sessionId, success)
        }
    }

    private fun handleTabletStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null) {
            val batteryLevel = messageData["batteryLevel"] as? Int
            val orientation = messageData["orientation"] as? String

            logger.debug("Status update from tablet ${tabletConnection.tablet.id}: battery=$batteryLevel, orientation=$orientation")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val removedTablet = tabletConnections.values.find { it.session == session }
        removedTablet?.let { connection ->
            tabletConnections.remove(connection.tablet.id)
            logger.info("Tablet disconnected: ${connection.tablet.friendlyName} (${connection.tablet.id})")
            auditService.logTabletConnection(connection.tablet.id, connection.tenantId, "DISCONNECTED")
        }

        val removedWorkstation = workstationConnections.values.find { it.session == session }
        removedWorkstation?.let { connection ->
            workstationConnections.remove(connection.workstationId)
            logger.info("Workstation disconnected: ${connection.workstationId}")
            auditService.logWorkstationConnection(connection.workstationId, connection.tenantId, connection.userId, "DISCONNECTED")
        }

        logger.debug("WebSocket connection closed: ${status.reason}")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error("WebSocket transport error from ${session.remoteAddress}", exception)

        tabletConnections.values.find { it.session == session }?.let { connection ->
            auditService.logTabletConnection(connection.tablet.id, connection.tenantId, "ERROR", exception.message)
        }

        workstationConnections.values.find { it.session == session }?.let { connection ->
            auditService.logWorkstationConnection(connection.workstationId, connection.tenantId, connection.userId, "ERROR", exception.message)
        }
    }

    fun sendSignatureRequest(tabletId: UUID, message: SignatureRequestMessage): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen) {
            val success = sendToSession(connection.session, message)
            if (success) {
                auditService.logSignatureRequest(connection.tenantId, message.sessionId, "SENT_TO_TABLET")
            }
            success
        } else {
            logger.warn("Tablet $tabletId not connected or session closed")
            false
        }
    }

    fun notifyWorkstation(workstationId: UUID, message: SignatureCompletedMessage) {
        val connection = workstationConnections[workstationId]
        if (connection != null && connection.session.isOpen) {
            sendToSession(connection.session, message)
            auditService.logWorkstationNotification(workstationId, connection.tenantId, message.type)
        } else {
            logger.warn("Workstation $workstationId not connected")
        }
    }

    fun sendTestRequest(tabletId: UUID) {
        val connection = tabletConnections[tabletId]
        if (connection != null && connection.session.isOpen) {
            val testMessage = SignatureRequestMessage(
                sessionId = "test-${UUID.randomUUID()}",
                tenantId = connection.tenantId,
                workstationId = UUID.randomUUID(),
                customerName = "Test Customer",
                vehicleInfo = VehicleInfoWS(
                    make = "Test",
                    model = "Test",
                    licensePlate = "TEST-123"
                ),
                serviceType = "Test Service",
                documentType = "Test Document"
            )
            sendToSession(connection.session, testMessage)
        }
    }

    fun broadcastShutdownNotification() {
        val shutdownMessage = ErrorMessage("Server shutting down", "Please reconnect after server restart")

        tabletConnections.values.forEach { connection ->
            sendToSession(connection.session, shutdownMessage)
        }

        workstationConnections.values.forEach { connection ->
            sendToSession(connection.session, shutdownMessage)
        }
    }

    fun closeAllConnections() {
        tabletConnections.values.forEach { connection ->
            try {
                connection.session.close(CloseStatus.GOING_AWAY.withReason("Server shutdown"))
            } catch (e: Exception) {
                logger.warn("Error closing tablet connection", e)
            }
        }

        workstationConnections.values.forEach { connection ->
            try {
                connection.session.close(CloseStatus.GOING_AWAY.withReason("Server shutdown"))
            } catch (e: Exception) {
                logger.warn("Error closing workstation connection", e)
            }
        }

        tabletConnections.clear()
        workstationConnections.clear()
    }

    private fun cleanupStaleConnections() {
        val now = Instant.now()
        val staleThreshold = Duration.ofMinutes(2)

        val staleTablets = tabletConnections.values.filter { connection ->
            Duration.between(connection.lastHeartbeat, now) > staleThreshold || !connection.session.isOpen
        }

        staleTablets.forEach { connection ->
            logger.info("Removing stale tablet connection: ${connection.tablet.id}")
            tabletConnections.remove(connection.tablet.id)
            try {
                if (connection.session.isOpen) {
                    connection.session.close(CloseStatus.SESSION_NOT_RELIABLE)
                }
            } catch (e: Exception) {
                logger.warn("Error closing stale tablet session", e)
            }
        }

        val staleWorkstations = workstationConnections.values.filter { connection ->
            Duration.between(connection.lastHeartbeat, now) > staleThreshold || !connection.session.isOpen
        }

        staleWorkstations.forEach { connection ->
            logger.info("Removing stale workstation connection: ${connection.workstationId}")
            workstationConnections.remove(connection.workstationId)
            try {
                if (connection.session.isOpen) {
                    connection.session.close(CloseStatus.SESSION_NOT_RELIABLE)
                }
            } catch (e: Exception) {
                logger.warn("Error closing stale workstation session", e)
            }
        }
    }

    private fun sendHeartbeats() {
        val heartbeat = HeartbeatMessage()

        tabletConnections.values.forEach { connection ->
            if (connection.session.isOpen) {
                sendToSession(connection.session, heartbeat)
            }
        }

        workstationConnections.values.forEach { connection ->
            if (connection.session.isOpen) {
                sendToSession(connection.session, heartbeat)
            }
        }
    }

    private fun sendToSession(session: WebSocketSession, message: WebSocketMessage): Boolean {
        return try {
            if (session.isOpen) {
                val json = objectMapper.writeValueAsString(mapOf(
                    "type" to message.type,
                    "payload" to message
                ))
                session.sendMessage(TextMessage(json))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Error sending WebSocket message to ${session.remoteAddress}", e)
            false
        }
    }

    // Public methods for monitoring
    fun getActiveConnectionsCount(): Int = tabletConnections.size + workstationConnections.size
    fun getActiveTabletsCount(): Int = tabletConnections.size
    fun getActiveWorkstationsCount(): Int = workstationConnections.size

    fun isTabletConnected(tabletId: UUID): Boolean {
        val connection = tabletConnections[tabletId]
        return connection?.session?.isOpen == true
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