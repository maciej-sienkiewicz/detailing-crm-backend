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
import java.net.URI
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
            val uri = session.uri!!
            logger.info("WebSocket connection attempt from: ${session.remoteAddress} to: $uri")

            when {
                uri.toString().contains("/ws/tablet/") -> handleTabletConnectionEstablished(session, uri)
                uri.toString().contains("/ws/workstation/") -> handleWorkstationConnection(session, uri)
                else -> {
                    logger.warn("Unknown WebSocket connection type: $uri")
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unknown connection type"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error establishing WebSocket connection from ${session.remoteAddress}", e)
            session.close(CloseStatus.SERVER_ERROR.withReason("Server error"))
        }
    }

    private fun handleTabletConnectionEstablished(session: WebSocketSession, uri: URI) {
        val deviceId = extractDeviceIdFromPath(uri.path)
            ?: throw SecurityException("Missing device ID in path")

        logger.info("WebSocket handshake successful: $uri")
        logger.info("Tablet connection attempt for device: $deviceId")

        // POPRAWKA: Nie sprawdzamy headers na początku - czekamy na authentication message
        // Tworzymy tymczasowe połączenie
        val tempConnection = TabletConnection(
            session = session,
            tablet = null, // Will be set after authentication
            tenantId = null,
            locationId = null,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = false
        )

        // Store with deviceId as key for now
        tabletConnections[deviceId] = tempConnection

        logger.info("Tablet pre-connected: $deviceId, awaiting authentication")

        // Send connection confirmation
        sendToSession(session, mapOf(
            "type" to "connection",
            "payload" to mapOf(
                "status" to "connected",
                "authenticated" to false,
                "timestamp" to Instant.now(),
                "deviceId" to deviceId,
                "message" to "Please send authentication"
            )
        ))
    }

    private fun handleWorkstationConnection(session: WebSocketSession, uri: URI) {
        val workstationId = extractWorkstationIdFromPath(uri.path)
            ?: throw SecurityException("Missing workstation ID in path")

        logger.info("Workstation connection attempt for: $workstationId")

        val internalWorkstationId = try {
            UUID.fromString(workstationId)
        } catch (e: IllegalArgumentException) {
            UUID.nameUUIDFromBytes(workstationId.toByteArray())
        }

        val connection = WorkstationConnection(
            session = session,
            workstationId = internalWorkstationId,
            originalWorkstationId = workstationId,
            companyId = null,
            userId = null,
            username = null,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = false
        )

        workstationConnections[internalWorkstationId] = connection

        logger.info("Workstation pre-connected: $workstationId (internal: $internalWorkstationId, awaiting authentication)")

        sendToSession(session, mapOf(
            "type" to "connection",
            "payload" to mapOf(
                "status" to "connected",
                "authenticated" to false,
                "timestamp" to Instant.now(),
                "workstationId" to workstationId,
                "internalId" to internalWorkstationId,
                "message" to "Please authenticate"
            )
        ))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            @Suppress("UNCHECKED_CAST")
            val messageData = objectMapper.readValue(message.payload, Map::class.java) as Map<String, Any>
            val messageType = messageData["type"] as? String

            logger.debug("WebSocket message received from ${session.remoteAddress}: type=$messageType")

            when (messageType) {
                "authentication" -> handleAuthentication(session, messageData)
                "connection" -> handleConnectionMessage(session, messageData)
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
                        "payload" to mapOf("error" to "Unknown message type: $messageType")
                    ))
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message from ${session.remoteAddress}", e)
            sendToSession(session, mapOf(
                "type" to "error",
                "payload" to mapOf("error" to "Invalid message format: ${e.message}")
            ))
        }
    }

    private fun handleAuthentication(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val token = payload?.get("token") as? String
        val deviceId = payload?.get("deviceId") as? String

        logger.info("Authentication attempt from ${session.remoteAddress}")

        if (token == null) {
            logger.warn("Authentication attempt without token from ${session.remoteAddress}")
            sendToSession(session, mapOf(
                "type" to "error",
                "payload" to mapOf("error" to "Missing authentication token")
            ))
            return
        }

        try {
            // Validate JWT token
            if (!jwtService.validateToken(token)) {
                logger.warn("Invalid JWT token received from ${session.remoteAddress}")
                throw SecurityException("Invalid JWT token")
            }

            val tokenType = jwtService.getTokenType(token)
            logger.debug("Token type: $tokenType for ${session.remoteAddress}")

            when (tokenType) {
                TokenType.TABLET -> handleTabletAuthentication(session, token, deviceId)
                TokenType.USER -> handleWorkstationAuthentication(session, token)
                TokenType.UNKNOWN -> {
                    logger.warn("Unknown token type from ${session.remoteAddress}")
                    throw SecurityException("Unknown token type")
                }
            }

        } catch (e: Exception) {
            logger.error("Authentication failed from ${session.remoteAddress}: ${e.message}")
            sendToSession(session, mapOf(
                "type" to "authentication",
                "payload" to mapOf(
                    "status" to "failed",
                    "error" to "Authentication failed: ${e.message}",
                    "timestamp" to Instant.now()
                )
            ))

            // Close connection after failed authentication
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication failed"))
            } catch (closeException: Exception) {
                logger.warn("Error closing session after authentication failure", closeException)
            }
        }
    }

    private fun handleTabletAuthentication(session: WebSocketSession, token: String, providedDeviceId: String?) {
        val tabletClaims = jwtService.extractTabletClaims(token)
        val deviceId = tabletClaims.deviceId

        // Verify device ID matches if provided
        if (providedDeviceId != null && UUID.fromString(providedDeviceId) != deviceId) {
            throw SecurityException("Device ID mismatch: provided=$providedDeviceId, token=$deviceId")
        }

        // Find tablet in database
        val tablet = tabletDeviceRepository.findById(deviceId)
            .orElseThrow { SecurityException("Tablet not found: $deviceId") }

        // Verify tenant ID matches
        if (tablet.tenantId != tabletClaims.tenantId) {
            throw SecurityException("Tenant mismatch: tablet=${tablet.tenantId}, token=${tabletClaims.tenantId}")
        }

        // Check if tablet is active
        if (tablet.status != DeviceStatus.ACTIVE) {
            throw SecurityException("Tablet not active: ${tablet.status}")
        }

        // Update connection with authenticated data
        val authenticatedConnection = TabletConnection(
            session = session,
            tablet = tablet,
            tenantId = tablet.tenantId,
            locationId = tablet.locationId,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = true
        )

        tabletConnections[deviceId] = authenticatedConnection
        tabletConnectionService.updateTabletLastSeen(deviceId)

        logger.info("Tablet authenticated successfully: ${tablet.friendlyName} (${deviceId}) from ${session.remoteAddress}")
        auditService.logTabletConnection(deviceId, tablet.tenantId, "AUTHENTICATED")

        // Send authentication success
        sendToSession(session, mapOf(
            "type" to "authentication",
            "payload" to mapOf(
                "status" to "authenticated",
                "timestamp" to Instant.now(),
                "deviceId" to deviceId,
                "tenantId" to tablet.tenantId,
                "locationId" to tablet.locationId,
                "tabletName" to tablet.friendlyName
            )
        ))
    }

    private fun handleWorkstationAuthentication(session: WebSocketSession, token: String) {
        val userClaims = jwtService.extractUserClaims(token)

        // Find workstation connection and update it
        val workstationConnection = workstationConnections.values.find { it.session == session }
        if (workstationConnection != null) {
            val authenticatedConnection = workstationConnection.copy(
                companyId = userClaims.companyId,
                userId = userClaims.userId,
                username = userClaims.username,
                authenticated = true
            )

            workstationConnections[workstationConnection.workstationId] = authenticatedConnection

            logger.info("Workstation authenticated: ${workstationConnection.originalWorkstationId} by user ${userClaims.username} (${userClaims.userId})")
            auditService.logWorkstationConnection(workstationConnection.workstationId, userClaims.companyId, userClaims.userId, "AUTHENTICATED")

            sendToSession(session, mapOf(
                "type" to "authentication",
                "payload" to mapOf(
                    "status" to "authenticated",
                    "timestamp" to Instant.now(),
                    "workstationId" to workstationConnection.originalWorkstationId,
                    "internalId" to workstationConnection.workstationId,
                    "userId" to userClaims.userId,
                    "username" to userClaims.username,
                    "roles" to userClaims.roles,
                    "permissions" to userClaims.permissions
                )
            ))
        } else {
            logger.error("Workstation connection not found for session from ${session.remoteAddress}")
            throw SecurityException("Workstation connection not found")
        }
    }

    private fun handleConnectionMessage(session: WebSocketSession, messageData: Map<String, Any>) {
        logger.debug("Connection message received from ${session.remoteAddress}")
    }

    private fun handleHeartbeat(session: WebSocketSession) {
        // Update tablet heartbeat
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections[connection.tablet!!.id] = connection.copy(lastHeartbeat = Instant.now())
            tabletConnectionService.updateTabletLastSeen(connection.tablet!!.id)
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

        if (tabletConnection != null && sessionId != null && tabletConnection.authenticated) {
            logger.info("Signature completion acknowledgment received from tablet ${tabletConnection.tablet!!.id} for session: $sessionId")
            auditService.logSignatureAcknowledgment(tabletConnection.tablet!!.id, sessionId, success)
        }
    }

    private fun handleTabletStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && payload != null && tabletConnection.authenticated) {
            val batteryLevel = payload["batteryLevel"] as? Int
            val orientation = payload["orientation"] as? String

            logger.debug("Status update from tablet ${tabletConnection.tablet!!.id}: battery=$batteryLevel, orientation=$orientation")
        }
    }

    private fun handleWorkstationStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val workstationConnection = workstationConnections.values.find { it.session == session }

        if (workstationConnection != null && payload != null && workstationConnection.authenticated) {
            logger.debug("Status update from workstation ${workstationConnection.workstationId} by user ${workstationConnection.username}")
        }
    }

    // Rest of the methods remain the same...
    fun sendSignatureRequest(tabletId: UUID, request: SignatureRequestDto): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen && connection.authenticated) {
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
                logger.info("Signature request sent to tablet $tabletId for session ${request.sessionId}")
                auditService.logSignatureRequest(connection.tenantId!!, request.sessionId, "SENT_TO_TABLET")
            } else {
                logger.warn("Failed to send signature request to tablet $tabletId")
            }
            success
        } else {
            logger.warn("Tablet $tabletId not connected, not authenticated, or session closed")
            false
        }
    }

    fun notifyWorkstation(workstationId: UUID, sessionId: String, success: Boolean, signedAt: Instant?) {
        val connection = workstationConnections[workstationId]
        if (connection != null && connection.session.isOpen && connection.authenticated) {
            val message = mapOf(
                "type" to "signature_completed",
                "payload" to mapOf(
                    "sessionId" to sessionId,
                    "success" to success,
                    "signedAt" to signedAt,
                    "timestamp" to Instant.now()
                )
            )
            val sent = sendToSession(connection.session, message)
            if (sent) {
                logger.info("Notified workstation $workstationId about session $sessionId completion (success: $success)")
                connection.companyId?.let { companyId ->
                    auditService.logWorkstationNotification(workstationId, companyId, "signature_completed")
                }
            }
        } else {
            logger.warn("Workstation $workstationId not connected or not authenticated")
        }
    }

    private fun sendToSession(session: WebSocketSession, message: Map<String, Any>): Boolean {
        return try {
            if (session.isOpen) {
                val json = objectMapper.writeValueAsString(message)
                session.sendMessage(TextMessage(json))
                true
            } else {
                logger.warn("Cannot send message - session is closed")
                false
            }
        } catch (e: Exception) {
            logger.error("Error sending WebSocket message to ${session.remoteAddress}", e)
            false
        }
    }

    // Monitoring methods
    fun getActiveConnectionsCount(): Int = tabletConnections.size + workstationConnections.size
    fun getActiveTabletsCount(): Int = tabletConnections.values.count { it.authenticated }
    fun getActiveWorkstationsCount(): Int = workstationConnections.values.count { it.authenticated }

    fun isTabletConnected(tabletId: UUID): Boolean {
        val connection = tabletConnections[tabletId]
        return connection?.session?.isOpen == true && connection.authenticated
    }

    private fun cleanupStaleConnections() {
        val now = Instant.now()
        val staleThreshold = Duration.ofMinutes(2)

        // Cleanup stale tablets
        val staleTablets = tabletConnections.values.filter { connection ->
            Duration.between(connection.lastHeartbeat, now) > staleThreshold || !connection.session.isOpen
        }

        staleTablets.forEach { connection ->
            logger.info("Removing stale tablet connection: ${connection.tablet?.id}")
            tabletConnections.remove(connection.tablet?.id)
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

    private fun extractDeviceIdFromPath(path: String): UUID? {
        return try {
            val parts = path.split("/")
            val deviceIdIndex = parts.indexOf("tablet") + 1
            if (deviceIdIndex < parts.size && deviceIdIndex > 0) {
                UUID.fromString(parts[deviceIdIndex])
            } else null
        } catch (e: Exception) {
            logger.error("Error extracting device ID from path: $path", e)
            null
        }
    }

    private fun extractWorkstationIdFromPath(path: String): String? {
        return try {
            val parts = path.split("/")
            val workstationIdIndex = parts.indexOf("workstation") + 1
            if (workstationIdIndex < parts.size && workstationIdIndex > 0) {
                parts[workstationIdIndex]
            } else null
        } catch (e: Exception) {
            logger.error("Error extracting workstation ID from path: $path", e)
            null
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // Remove tablet connection
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections.remove(connection.tablet?.id)
            logger.info("Tablet disconnected: ${connection.tablet?.id} (${status.code}: ${status.reason})")
            if (connection.authenticated) {
                auditService.logTabletConnection(connection.tablet!!.id, connection.tenantId!!, "DISCONNECTED")
            }
        }

        // Remove workstation connection
        workstationConnections.values.find { it.session == session }?.let { connection ->
            workstationConnections.remove(connection.workstationId)
            logger.info("Workstation disconnected: ${connection.workstationId} (${status.code}: ${status.reason})")
            if (connection.authenticated && connection.companyId != null && connection.userId != null) {
                auditService.logWorkstationConnection(connection.workstationId, connection.companyId!!, connection.userId!!, "DISCONNECTED")
            }
        }
    }

    // Dodaj te metody do istniejącego SignatureWebSocketHandler.kt

// W klasie SignatureWebSocketHandler dodaj na końcu:

    /**
     * Disconnect specific tablet
     */
    fun disconnectTablet(tabletId: UUID): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen) {
            try {
                // Send disconnect message
                sendToSession(connection.session, mapOf(
                    "type" to "disconnect",
                    "payload" to mapOf(
                        "reason" to "Administrative disconnect",
                        "timestamp" to Instant.now()
                    )
                ))

                // Close connection
                connection.session.close(CloseStatus.NORMAL.withReason("Administrative disconnect"))

                // Remove from connections map
                tabletConnections.remove(tabletId)

                logger.info("Tablet $tabletId disconnected administratively")
                true
            } catch (e: Exception) {
                logger.error("Error disconnecting tablet $tabletId", e)
                false
            }
        } else {
            logger.warn("Tablet $tabletId not connected, cannot disconnect")
            false
        }
    }

    /**
     * Get list of all connected tablets
     */
    fun getConnectedTablets(): List<UUID> {
        return tabletConnections.keys.toList()
    }

    /**
     * Get connection info for specific tablet
     */
    fun getTabletConnectionInfo(tabletId: UUID): Map<String, Any>? {
        val connection = tabletConnections[tabletId]
        return if (connection != null) {
            mapOf(
                "deviceId" to tabletId,
                "tenantId" to (connection.tenantId ?: "unknown"),
                "locationId" to (connection.locationId ?: "unknown"),
                "isAuthenticated" to connection.authenticated,
                "connectedAt" to connection.connectedAt,
                "lastHeartbeat" to connection.lastHeartbeat,
                "tabletName" to (connection.tablet?.friendlyName ?: "unknown"),
                "sessionOpen" to connection.session.isOpen
            )
        } else null
    }

    /**
     * Get all tablets connection status
     */
    fun getAllTabletsStatus(): Map<UUID, Map<String, Any>> {
        return tabletConnections.mapValues { (tabletId, connection) ->
            mapOf(
                "isOnline" to connection.session.isOpen,
                "isAuthenticated" to connection.authenticated,
                "connectedAt" to connection.connectedAt,
                "lastHeartbeat" to connection.lastHeartbeat,
                "tabletName" to (connection.tablet?.friendlyName ?: "unknown"),
                "tenantId" to (connection.tenantId ?: "unknown")
            )
        }
    }

    /**
     * Send administrative message to tablet
     */
    fun sendAdminMessage(tabletId: UUID, messageType: String, data: Map<String, Any>): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen && connection.authenticated) {
            val message = mapOf(
                "type" to "admin_message",
                "payload" to mapOf(
                    "messageType" to messageType,
                    "data" to data,
                    "timestamp" to Instant.now()
                )
            )

            val success = sendToSession(connection.session, message)
            if (success) {
                logger.info("Admin message '$messageType' sent to tablet $tabletId")
            } else {
                logger.warn("Failed to send admin message to tablet $tabletId")
            }
            success
        } else {
            logger.warn("Cannot send admin message to tablet $tabletId - not connected or not authenticated")
            false
        }
    }

    /**
     * Ping specific tablet
     */
    fun pingTablet(tabletId: UUID): Boolean {
        return sendAdminMessage(tabletId, "ping", mapOf(
            "requestId" to UUID.randomUUID().toString(),
            "timestamp" to Instant.now()
        ))
    }

    /**
     * Request tablet status update
     */
    fun requestTabletStatus(tabletId: UUID): Boolean {
        return sendAdminMessage(tabletId, "status_request", mapOf(
            "requestId" to UUID.randomUUID().toString(),
            "requestedFields" to listOf("battery", "orientation", "performance"),
            "timestamp" to Instant.now()
        ))
    }

    /**
     * Broadcast message to all connected tablets
     */
    fun broadcastToTablets(messageType: String, data: Map<String, Any>): Int {
        var sentCount = 0

        tabletConnections.values.forEach { connection ->
            if (connection.session.isOpen && connection.authenticated) {
                val message = mapOf(
                    "type" to "broadcast",
                    "payload" to mapOf(
                        "messageType" to messageType,
                        "data" to data,
                        "timestamp" to Instant.now()
                    )
                )

                if (sendToSession(connection.session, message)) {
                    sentCount++
                }
            }
        }

        logger.info("Broadcast message '$messageType' sent to $sentCount tablets")
        return sentCount
    }

    /**
     * Get tablets by tenant
     */
    fun getTabletsByTenant(tenantId: UUID): List<UUID> {
        return tabletConnections.values
            .filter { it.tenantId == tenantId && it.authenticated }
            .map { it.tablet?.id }
            .filterNotNull()
    }

    /**
     * Check if tenant has any connected tablets
     */
    fun hasTenantConnectedTablets(tenantId: UUID): Boolean {
        return tabletConnections.values.any {
            it.tenantId == tenantId && it.authenticated && it.session.isOpen
        }
    }

    /**
     * Get connection statistics
     */
    fun getConnectionStatistics(): Map<String, Any> {
        val now = Instant.now()
        val staleThreshold = Duration.ofMinutes(2)

        val totalConnections = tabletConnections.size
        val authenticatedConnections = tabletConnections.values.count { it.authenticated }
        val activeConnections = tabletConnections.values.count {
            it.session.isOpen && Duration.between(it.lastHeartbeat, now) < staleThreshold
        }

        val tenantStats = tabletConnections.values
            .filter { it.authenticated }
            .groupBy { it.tenantId }
            .mapValues { (_, connections) -> connections.size }

        return mapOf(
            "totalConnections" to totalConnections,
            "authenticatedConnections" to authenticatedConnections,
            "activeConnections" to activeConnections,
            "staleConnections" to (totalConnections - activeConnections),
            "tenantStats" to tenantStats,
            "timestamp" to now
        )
    }
}

// Updated data classes
data class TabletConnection(
    val session: WebSocketSession,
    val tablet: TabletDevice?,
    val tenantId: UUID?,
    val locationId: UUID?,
    val connectedAt: Instant,
    val lastHeartbeat: Instant,
    val authenticated: Boolean = false
)

data class WorkstationConnection(
    val session: WebSocketSession,
    val workstationId: UUID,
    val originalWorkstationId: String,
    val companyId: Long?,
    val userId: Long?,
    val username: String?,
    val connectedAt: Instant,
    val lastHeartbeat: Instant,
    val authenticated: Boolean = false
)

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