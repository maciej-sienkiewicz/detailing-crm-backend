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
                uri.toString().contains("/ws/tablet/") -> handleTabletConnection(session, uri)
                uri.toString().contains("/ws/workstation/") -> handleWorkstationConnection(session, uri)
                else -> {
                    logger.warn("Unknown WebSocket connection type: $uri")
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unknown connection type"))
                }
            }
        } catch (e: SecurityException) {
            logger.error("Security error establishing WebSocket connection from ${session.remoteAddress}", e)
            auditService.logSecurityViolation("websocket_unauthorized", session.remoteAddress?.toString(), e.message)
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication failed"))
        } catch (e: Exception) {
            logger.error("Error establishing WebSocket connection from ${session.remoteAddress}", e)
            session.close(CloseStatus.SERVER_ERROR.withReason("Server error"))
        }
    }

    private fun handleTabletConnection(session: WebSocketSession, uri: URI) {
        val deviceId = extractDeviceIdFromPath(uri.path)
            ?: throw SecurityException("Missing device ID in path")

        logger.info("Tablet connection attempt for device: $deviceId")

        // Tablet authentication - sprawdź nagłówki
        val token = session.handshakeHeaders.getFirst("X-Device-Token")
            ?: throw SecurityException("Missing X-Device-Token header")

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
                throw SecurityException("Device ID mismatch: path=$deviceId, token=${tabletClaims.deviceId}")
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

            logger.info("Tablet connected successfully: ${tablet.friendlyName} (${deviceId}) from ${session.remoteAddress}")
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

    private fun handleWorkstationConnection(session: WebSocketSession, uri: URI) {
        // Nie wymagamy UUID dla workstation - używamy string ID
        val workstationId = extractWorkstationIdFromPath(uri.path)
            ?: throw SecurityException("Missing workstation ID in path")

        logger.info("Workstation connection attempt for: $workstationId")

        // Dla workstation nie wymagamy tokenu w headers na początku
        // Token może być przesłany w pierwszej wiadomości
        // Generujemy UUID dla wewnętrznego użytku
        val internalWorkstationId = try {
            UUID.fromString(workstationId)
        } catch (e: IllegalArgumentException) {
            // Jeśli workstationId nie jest UUID, generujemy deterministyczne UUID
            UUID.nameUUIDFromBytes(workstationId.toByteArray())
        }

        val connection = WorkstationConnection(
            session = session,
            workstationId = internalWorkstationId,
            originalWorkstationId = workstationId, // Dodaj pole dla oryginalnego ID
            companyId = null, // Zostanie ustawione po autentykacji
            userId = null,
            username = null,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = false
        )

        workstationConnections[internalWorkstationId] = connection

        logger.info("Workstation pre-connected: $workstationId (internal: $internalWorkstationId, awaiting authentication)")

        // Send connection confirmation - ale nie authenticated
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
                "payload" to mapOf("error" to "Invalid message format")
            ))
        }
    }

    private fun handleAuthentication(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val token = payload?.get("token") as? String

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
                throw SecurityException("Invalid JWT token")
            }

            // Check if it's a user token (for workstation)
            if (jwtService.getTokenType(token) != TokenType.USER) {
                throw SecurityException("Invalid token type for workstation connection")
            }

            val userClaims = jwtService.extractUserClaims(token)

            // Relaxed permission check - sprawdź czy użytkownik ma jakąkolwiek rolę
            val hasValidRole = userClaims.roles.any { role ->
                role.uppercase() in listOf("ADMIN", "MANAGER", "USER", "EMPLOYEE")
            }

            val hasWorkstationPermission = userClaims.permissions.contains("workstation:connect")


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

        } catch (e: Exception) {
            logger.error("Authentication failed from ${session.remoteAddress}: ${e.message}", e)
            sendToSession(session, mapOf(
                "type" to "error",
                "payload" to mapOf("error" to "Authentication failed: ${e.message}")
            ))

            // Zamknij połączenie po nieudanej autentykacji
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication failed"))
            } catch (closeException: Exception) {
                logger.warn("Error closing session after authentication failure", closeException)
            }
        }
    }
    private fun handleConnectionMessage(session: WebSocketSession, messageData: Map<String, Any>) {
        // Handle connection status messages
        logger.debug("Connection message received from ${session.remoteAddress}")
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
                logger.info("Signature request sent to tablet $tabletId for session ${request.sessionId}")
                auditService.logSignatureRequest(connection.tenantId, request.sessionId, "SENT_TO_TABLET")
            } else {
                logger.warn("Failed to send signature request to tablet $tabletId")
            }
            success
        } else {
            logger.warn("Tablet $tabletId not connected or session closed")
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
    fun getActiveTabletsCount(): Int = tabletConnections.size
    fun getActiveWorkstationsCount(): Int = workstationConnections.values.count { it.authenticated }

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

    private fun extractDeviceIdFromPath(path: String): UUID? {
        return try {
            // Path format: /ws/tablet/{deviceId}
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
            // Path format: /ws/workstation/{workstationId}
            val parts = path.split("/")
            val workstationIdIndex = parts.indexOf("workstation") + 1
            if (workstationIdIndex < parts.size && workstationIdIndex > 0) {
                parts[workstationIdIndex] // Zwróć cały string, nie próbuj parsować jako UUID
            } else null
        } catch (e: Exception) {
            logger.error("Error extracting workstation ID from path: $path", e)
            null
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // Remove tablet connection
        tabletConnections.values.find { it.session == session }?.let { connection ->
            tabletConnections.remove(connection.tablet.id)
            logger.info("Tablet disconnected: ${connection.tablet.id} (${status.code}: ${status.reason})")
            auditService.logTabletConnection(connection.tablet.id, connection.tenantId, "DISCONNECTED")
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
}

// Updated DTOs

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
    val workstationId: UUID,           // UUID dla wewnętrznego użytku
    val originalWorkstationId: String, // Oryginalny string ID z frontendu
    val companyId: Long?,              // Nullable until authenticated
    val userId: Long?,                 // Nullable until authenticated
    val username: String?,             // Nullable until authenticated
    val connectedAt: Instant,
    val lastHeartbeat: Instant,
    val authenticated: Boolean = false
)