package com.carslab.crm.signature.websocket

import com.carslab.crm.audit.service.AuditService
import com.carslab.crm.security.JwtService
import com.carslab.crm.security.TokenType
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.service.TabletConnectionService
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

@Component
class SignatureWebSocketHandler(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val tabletConnectionService: TabletConnectionService,
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

        // Tablet authentication - compatible with new JWT system
        val token = session.handshakeHeaders.getFirst("X-Device-Token")
            ?: throw SecurityException("Missing device token")

        // Validate JWT token using new JwtService
        if (!jwtService.validateToken(token)) {
            throw SecurityException("Invalid device token")
        }

        // Check if it's a tablet token
        if (jwtService.getTokenType(token) != TokenType.TABLET) {
            throw SecurityException("Invalid token type for tablet connection")
        }

        try {
            val tabletClaims = jwtService.extractTabletClaims(token)

            // Verify device ID matches token
            if (tabletClaims.deviceId != deviceId) {
                throw SecurityException("Device ID mismatch")
            }

            // Find tablet in database
            val tablet = tabletDeviceRepository.findById(deviceId)
                .orElseThrow { SecurityException("Tablet not found") }

            // Verify tenant ID matches
            if (tablet.tenantId != tabletClaims.tenantId) {
                throw SecurityException("Tenant mismatch")
            }

            // Check if tablet is active
            if (tablet.status != DeviceStatus.ACTIVE) {
                throw SecurityException("Tablet not active")
            }

            // Close existing connection if any
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
            tabletConnectionService.updateTabletLastSeen(deviceId)

            logger.info("Tablet connected: ${tablet.friendlyName} (${deviceId}) from ${session.remoteAddress}")
            auditService.logTabletConnection(deviceId, tablet.tenantId, "CONNECTED")

            // Send connection confirmation
            sendToSession(session, mapOf(
                "type" to "connection",
                "payload" to mapOf(
                    "status" to "connected",
                    "timestamp" to Instant.now(),
                    "deviceId" to deviceId,
                    "tenantId" to tablet.tenantId
                )
            ))

        } catch (e: Exception) {
            logger.error("Failed to extract tablet claims from token", e)
            throw SecurityException("Invalid tablet token claims")
        }
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

        // Validate JWT token using new JwtService
        if (!jwtService.validateToken(token)) {
            throw SecurityException("Invalid JWT token")
        }

        // Check if it's a user token
        if (jwtService.getTokenType(token) != TokenType.USER) {
            throw SecurityException("Invalid token type for workstation connection")
        }

        try {
            val userClaims = jwtService.extractUserClaims(token)

            // Check permissions (assuming permissions are set correctly)
            if (!userClaims.permissions.contains("workstation:connect") &&
                !userClaims.roles.any { it.uppercase() in listOf("ADMIN", "MANAGER", "USER") }) {
                throw SecurityException("Insufficient permissions for workstation connection")
            }

            val connection = WorkstationConnection(
                session = session,
                workstationId = workstationId,
                companyId = userClaims.companyId,
                userId = userClaims.userId,
                username = userClaims.username,
                connectedAt = Instant.now(),
                lastHeartbeat = Instant.now()
            )

            workstationConnections[workstationId] = connection

            logger.info("Workstation connected: $workstationId by user ${userClaims.username} (${userClaims.userId})")
            auditService.logWorkstationConnection(workstationId, userClaims.companyId, userClaims.userId, "CONNECTED")

            sendToSession(session, mapOf(
                "type" to "connection",
                "payload" to mapOf(
                    "status" to "connected",
                    "timestamp" to Instant.now(),
                    "workstationId" to workstationId,
                    "userId" to userClaims.userId,
                    "username" to userClaims.username
                )
            ))

        } catch (e: Exception) {
            logger.error("Failed to extract user claims from token", e)
            throw SecurityException("Invalid user token claims")
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            @Suppress("UNCHECKED_CAST")
            val messageData = objectMapper.readValue(message.payload, Map::class.java) as Map<String, Any>
            val messageType = messageData["type"] as? String

            when (messageType) {
                "heartbeat" -> handleHeartbeat(session)
                "signature_completed" -> {
                    @Suppress("UNCHECKED_CAST")
                    handleSignatureCompleted(session, messageData as Map<String, Any>)
                }
                "tablet_status" -> {
                    @Suppress("UNCHECKED_CAST")
                    handleTabletStatusUpdate(session, messageData as Map<String, Any>)
                }
                "workstation_status" -> {
                    @Suppress("UNCHECKED_CAST")
                    handleWorkstationStatusUpdate(session, messageData as Map<String, Any>)
                }
                else -> {
                    logger.warn("Unknown message type: $messageType from ${session.remoteAddress}")
                    sendToSession(session, mapOf(
                        "type" to "error",
                        "payload" to mapOf("error" to "Unknown message type")
                    ))
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message from ${session.remoteAddress}", e)
            sendToSession(session, mapOf(
                "type" to "error",
                "payload" to mapOf("error" to "Invalid message format")
            ))
        }
    }

    private fun handleHeartbeat(session: WebSocketSession) {
        // Update tablet heartbeat
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections[connection.tablet.id] = connection.copy(lastHeartbeat = Instant.now())
            tabletConnectionService.updateTabletLastSeen(connection.tablet.id)
        }

        // Update workstation heartbeat
        workstationConnections.values.find { it.session == session }?.let { connection ->
            workstationConnections[connection.workstationId] = connection.copy(lastHeartbeat = Instant.now())
        }

        // Send heartbeat response
        sendToSession(session, mapOf(
            "type" to "heartbeat",
            "payload" to mapOf("timestamp" to Instant.now())
        ))
    }

    private fun handleSignatureCompleted(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val sessionId = payload?.get("sessionId") as? String
        val success = payload?.get("success") as? Boolean ?: false

        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && sessionId != null) {
            logger.info("Signature completion acknowledgment received from tablet ${tabletConnection.tablet.id} for session: $sessionId")
            auditService.logSignatureAcknowledgment(tabletConnection.tablet.id, sessionId, success)
        }
    }

    private fun handleTabletStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && payload != null) {
            val batteryLevel = payload["batteryLevel"] as? Int
            val orientation = payload["orientation"] as? String

            logger.debug("Status update from tablet ${tabletConnection.tablet.id}: battery=$batteryLevel, orientation=$orientation")
        }
    }

    private fun handleWorkstationStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val workstationConnection = workstationConnections.values.find { it.session == session }

        if (workstationConnection != null && payload != null) {
            logger.debug("Status update from workstation ${workstationConnection.workstationId} by user ${workstationConnection.username}")
        }
    }

    fun sendSignatureRequest(tabletId: UUID, request: SignatureRequestDto): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen) {
            val message = mapOf(
                "type" to "signature_request",
                "payload" to mapOf(
                    "sessionId" to request.sessionId,
                    "tenantId" to request.tenantId,
                    "workstationId" to request.workstationId,
                    "customerName" to request.customerName,
                    "vehicleInfo" to mapOf(
                        "make" to request.vehicleInfo.make,
                        "model" to request.vehicleInfo.model,
                        "licensePlate" to request.vehicleInfo.licensePlate,
                        "vin" to request.vehicleInfo.vin
                    ),
                    "serviceType" to request.serviceType,
                    "documentType" to request.documentType,
                    "timestamp" to Instant.now()
                )
            )

            val success = sendToSession(connection.session, message)
            if (success) {
                auditService.logSignatureRequest(connection.tenantId, request.sessionId, "SENT_TO_TABLET")
            }
            success
        } else {
            logger.warn("Tablet $tabletId not connected or session closed")
            false
        }
    }

    fun notifyWorkstation(workstationId: UUID, sessionId: String, success: Boolean, signedAt: Instant?) {
        val connection = workstationConnections[workstationId]
        if (connection != null && connection.session.isOpen) {
            val message = mapOf(
                "type" to "signature_completed",
                "payload" to mapOf(
                    "sessionId" to sessionId,
                    "success" to success,
                    "signedAt" to signedAt
                )
            )
            sendToSession(connection.session, message)
            auditService.logWorkstationNotification(workstationId, connection.companyId, "signature_completed")
        } else {
            logger.warn("Workstation $workstationId not connected")
        }
    }

    private fun sendToSession(session: WebSocketSession, message: Map<String, Any>): Boolean {
        return try {
            if (session.isOpen) {
                val json = objectMapper.writeValueAsString(message)
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

    // Monitoring methods
    fun getActiveConnectionsCount(): Int = tabletConnections.size + workstationConnections.size
    fun getActiveTabletsCount(): Int = tabletConnections.size
    fun getActiveWorkstationsCount(): Int = workstationConnections.size

    fun isTabletConnected(tabletId: UUID): Boolean {
        val connection = tabletConnections[tabletId]
        return connection?.session?.isOpen == true
    }

    private fun cleanupStaleConnections() {
        val now = Instant.now()
        val staleThreshold = Duration.ofMinutes(2)

        // Cleanup stale tablets
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

        // Cleanup stale workstations
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
        val heartbeat = mapOf(
            "type" to "heartbeat",
            "payload" to mapOf("timestamp" to Instant.now())
        )

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

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // Remove tablet connection
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections.remove(connection.tablet.id)
            logger.info("Tablet disconnected: ${connection.tablet.id}")
            auditService.logTabletConnection(connection.tablet.id, connection.tenantId, "DISCONNECTED")
        }

        // Remove workstation connection
        workstationConnections.values.find { it.session == session }?.let { connection ->
            workstationConnections.remove(connection.workstationId)
            logger.info("Workstation disconnected: ${connection.workstationId}")
            auditService.logWorkstationConnection(connection.workstationId, connection.companyId, connection.userId, "DISCONNECTED")
        }
    }
}

// DTOs compatible with frontend and backend
data class SignatureRequestDto(
    val sessionId: String,
    val tenantId: UUID,
    val workstationId: UUID,
    val customerName: String,
    val vehicleInfo: VehicleInfoDto,
    val serviceType: String,
    val documentType: String
)

data class VehicleInfoDto(
    val make: String,
    val model: String,
    val licensePlate: String,
    val vin: String? = null
)

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
    val companyId: Long,      // Changed from tenantId to companyId for compatibility
    val userId: Long,         // Changed from UUID to Long for compatibility
    val username: String,     // Added username for better logging
    val connectedAt: Instant,
    val lastHeartbeat: Instant
)