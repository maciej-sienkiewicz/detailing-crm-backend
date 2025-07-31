package com.carslab.crm.signature.websocket

import com.carslab.crm.audit.service.AuditService
import com.carslab.crm.security.JwtService
import com.carslab.crm.security.TokenType
import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.service.TabletConnectionService
import com.carslab.crm.infrastructure.events.EventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.security.MessageDigest
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
    private val jwtService: JwtService,
    private val eventPublisher: EventPublisher
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(this::class.java)
    val tabletConnections = ConcurrentHashMap<UUID, TabletConnection>()
    val workstationConnections = ConcurrentHashMap<UUID, WorkstationConnection>()
    private val heartbeatExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    init {
        heartbeatExecutor.scheduleAtFixedRate(::cleanupStaleConnections, 30, 30, TimeUnit.SECONDS)
        heartbeatExecutor.scheduleAtFixedRate(::sendHeartbeats, 25, 25, TimeUnit.SECONDS)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val uri = session.uri
            if (uri == null) {
                logger.warn("WebSocket session URI is null, closing connection")
                session.close(CloseStatus.BAD_DATA.withReason("Missing URI"))
                return
            }

            val uriString = uri.toString()
            logger.info("WebSocket connection attempt from: ${session.remoteAddress} to: $uriString")

            when {
                uriString.contains("/ws/tablet/") -> handleTabletConnectionEstablished(session, uri)
                uriString.contains("/ws/workstation/") -> handleWorkstationConnection(session, uri)
                else -> {
                    logger.warn("Unknown WebSocket connection type: $uriString")
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unknown connection type"))
                }
            }
        } catch (e: Exception) {
            logger.error("Error establishing WebSocket connection from ${session.remoteAddress}", e)
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Server error"))
            } catch (closeEx: Exception) {
                logger.warn("Error closing session after connection error", closeEx)
            }
        }
    }

    private fun handleTabletConnectionEstablished(session: WebSocketSession, uri: URI) {
        val deviceId = extractDeviceIdFromPath(uri.path)
        if (deviceId == null) {
            logger.warn("Missing device ID in tablet connection path: ${uri.path}")
            session.close(CloseStatus.BAD_DATA.withReason("Missing device ID"))
            return
        }

        logger.info("Tablet connection attempt for device: $deviceId")

        val tempConnection = TabletConnection(
            session = session,
            tablet = null,
            companyId = null,
            locationId = null,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = false
        )

        tabletConnections[deviceId] = tempConnection

        logger.info("Tablet pre-connected: $deviceId, awaiting authentication")

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
        if (workstationId == null) {
            logger.warn("Missing workstation ID in workstation connection path: ${uri.path}")
            session.close(CloseStatus.BAD_DATA.withReason("Missing workstation ID"))
            return
        }

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

        logger.info("Workstation pre-connected: $workstationId (internal: $internalWorkstationId), awaiting authentication")

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
                "document_signature_completed" -> {
                    @Suppress("UNCHECKED_CAST")
                    handleDocumentSignatureCompleted(session, messageData as Map<String, Any>)
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

        if (providedDeviceId != null && UUID.fromString(providedDeviceId) != deviceId) {
            throw SecurityException("Device ID mismatch: provided=$providedDeviceId, token=$deviceId")
        }

        val tablet = tabletDeviceRepository.findById(deviceId)
            .orElseThrow { SecurityException("Tablet not found: $deviceId") }

        if (tablet.companyId != tabletClaims.companyId) {
            throw SecurityException("Company mismatch: tablet=${tablet.companyId}, token=${tabletClaims.companyId}")
        }

        if (tablet.status != DeviceStatus.ACTIVE) {
            throw SecurityException("Tablet not active: ${tablet.status}")
        }

        val authenticatedConnection = TabletConnection(
            session = session,
            tablet = tablet,
            companyId = tablet.companyId,
            locationId = tablet.locationId,
            connectedAt = Instant.now(),
            lastHeartbeat = Instant.now(),
            authenticated = true
        )

        tabletConnections[deviceId] = authenticatedConnection
        tabletConnectionService.updateTabletLastSeen(deviceId)

        logger.info("Tablet authenticated successfully: ${tablet.friendlyName} (${deviceId}) from ${session.remoteAddress}")

        sendToSession(session, mapOf(
            "type" to "authentication",
            "payload" to mapOf(
                "status" to "authenticated",
                "timestamp" to Instant.now(),
                "deviceId" to deviceId,
                "companyId" to tablet.companyId,
                "locationId" to tablet.locationId,
                "tabletName" to tablet.friendlyName
            )
        ))
    }

    private fun handleWorkstationAuthentication(session: WebSocketSession, token: String) {
        val userClaims = jwtService.extractUserClaims(token)

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
        tabletConnections.values.find { it.session == session }?.let { connection ->
            if (connection.tablet != null) {
                tabletConnections[connection.tablet!!.id] = connection.copy(lastHeartbeat = Instant.now())
                tabletConnectionService.updateTabletLastSeen(connection.tablet!!.id)
            }
        }

        workstationConnections.values.find { it.session == session }?.let { connection ->
            workstationConnections[connection.workstationId] = connection.copy(lastHeartbeat = Instant.now())
        }

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

        if (tabletConnection != null && sessionId != null && tabletConnection.authenticated && tabletConnection.tablet != null) {
            logger.info("Signature completion acknowledgment received from tablet ${tabletConnection.tablet!!.id} for session: $sessionId")
            auditService.logSignatureAcknowledgment(tabletConnection.tablet!!.id, sessionId, success)
        }
    }

    private fun handleDocumentSignatureCompleted(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val sessionId = payload?.get("sessionId") as? String
        val signatureImage = payload?.get("signatureImage") as? String
        val success = payload?.get("success") as? Boolean ?: false

        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && sessionId != null && tabletConnection.authenticated && tabletConnection.tablet != null) {
            logger.info("Document signature completed received from tablet ${tabletConnection.tablet!!.id} for session: $sessionId")
            auditService.logSignatureAcknowledgment(tabletConnection.tablet!!.id, sessionId, success)
        } else {
            logger.warn("Document signature completed from unknown or unauthenticated tablet")
        }
    }

    private fun handleTabletStatusUpdate(session: WebSocketSession, messageData: Map<String, Any>) {
        val payload = messageData["payload"] as? Map<String, Any>
        val tabletConnection = tabletConnections.values.find { it.session == session }

        if (tabletConnection != null && payload != null && tabletConnection.authenticated && tabletConnection.tablet != null) {
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

    fun sendSignatureRequest(tabletId: UUID, request: SignatureRequestDto): Boolean {
        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen && connection.authenticated) {
            val message = mapOf(
                "type" to "signature_request",
                "payload" to mapOf(
                    "sessionId" to request.sessionId,
                    "companyId" to request.companyId,
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

    fun sendToSession(session: WebSocketSession, message: Map<String, Any>): Boolean {
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

        val staleTablets = tabletConnections.values.filter { connection ->
            try {
                Duration.between(connection.lastHeartbeat, now) > staleThreshold || !connection.session.isOpen
            } catch (e: Exception) {
                logger.warn("Error checking tablet connection staleness", e)
                true
            }
        }

        staleTablets.forEach { connection ->
            try {
                val tabletId = connection.tablet?.id
                if (tabletId != null) {
                    logger.info("Removing stale tablet connection: $tabletId")
                    tabletConnections.remove(tabletId)
                } else {
                    val entryToRemove = tabletConnections.entries.find { it.value.session == connection.session }
                    entryToRemove?.let {
                        logger.info("Removing stale tablet connection with null tablet: ${entryToRemove.key}")
                        tabletConnections.remove(entryToRemove.key)
                    }
                }

                if (connection.session.isOpen) {
                    connection.session.close(CloseStatus.SESSION_NOT_RELIABLE)
                }
            } catch (e: Exception) {
                logger.warn("Error removing stale tablet session", e)
            }
        }

        val staleWorkstations = workstationConnections.values.filter { connection ->
            try {
                Duration.between(connection.lastHeartbeat, now) > staleThreshold || !connection.session.isOpen
            } catch (e: Exception) {
                logger.warn("Error checking workstation connection staleness", e)
                true
            }
        }

        staleWorkstations.forEach { connection ->
            try {
                logger.info("Removing stale workstation connection: ${connection.workstationId}")
                workstationConnections.remove(connection.workstationId)

                if (connection.session.isOpen) {
                    connection.session.close(CloseStatus.SESSION_NOT_RELIABLE)
                }
            } catch (e: Exception) {
                logger.warn("Error removing stale workstation session", e)
            }
        }
    }

    private fun sendHeartbeats() {
        val heartbeat = mapOf(
            "type" to "heartbeat",
            "payload" to mapOf("timestamp" to Instant.now())
        )

        tabletConnections.values.forEach { connection ->
            try {
                if (connection.session.isOpen) {
                    sendToSession(connection.session, heartbeat)
                }
            } catch (e: Exception) {
                logger.warn("Error sending heartbeat to tablet", e)
            }
        }

        workstationConnections.values.forEach { connection ->
            try {
                if (connection.session.isOpen) {
                    sendToSession(connection.session, heartbeat)
                }
            } catch (e: Exception) {
                logger.warn("Error sending heartbeat to workstation", e)
            }
        }
    }

    private fun extractDeviceIdFromPath(path: String): UUID? {
        return try {
            val parts = path.split("/")
            val deviceIdIndex = parts.indexOf("tablet") + 1
            if (deviceIdIndex < parts.size && deviceIdIndex > 0) {
                UUID.fromString(parts[deviceIdIndex])
            } else {
                logger.warn("Device ID not found in path: $path")
                null
            }
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
            } else {
                logger.warn("Workstation ID not found in path: $path")
                null
            }
        } catch (e: Exception) {
            logger.error("Error extracting workstation ID from path: $path", e)
            null
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        try {
            val tabletEntryToRemove = tabletConnections.entries.find { it.value.session == session }
            if (tabletEntryToRemove != null) {
                val tabletId = tabletEntryToRemove.key
                tabletEntryToRemove.value

                if (tabletId != null) {
                    tabletConnections.remove(tabletId)
                    logger.info("Tablet disconnected: $tabletId (${status.code}: ${status.reason})")
                } else {
                    logger.warn("Tablet connection found but tabletId is null")
                }
            }

            val workstationEntryToRemove = workstationConnections.entries.find { it.value.session == session }
            if (workstationEntryToRemove != null) {
                val workstationId = workstationEntryToRemove.key
                val connection = workstationEntryToRemove.value

                if (workstationId != null) {
                    workstationConnections.remove(workstationId)
                    logger.info("Workstation disconnected: $workstationId (${status.code}: ${status.reason})")

                    if (connection.authenticated && connection.companyId != null && connection.userId != null) {
                        auditService.logWorkstationConnection(workstationId, connection.companyId!!, connection.userId!!, "DISCONNECTED")
                    }
                } else {
                    logger.warn("Workstation connection found but workstationId is null")
                }
            }

            if (tabletEntryToRemove == null && workstationEntryToRemove == null) {
                logger.debug("Connection closed for unknown session from ${session.remoteAddress}")
            }

        } catch (e: Exception) {
            logger.error("Error in afterConnectionClosed", e)
        }
    }

    fun disconnectTablet(tabletId: UUID): Boolean {
        if (tabletId == null) {
            logger.warn("Cannot disconnect tablet: tabletId is null")
            return false
        }

        val connection = tabletConnections[tabletId]
        return if (connection != null && connection.session.isOpen) {
            try {
                sendToSession(connection.session, mapOf(
                    "type" to "disconnect",
                    "payload" to mapOf(
                        "reason" to "Administrative disconnect",
                        "timestamp" to Instant.now()
                    )
                ))

                connection.session.close(CloseStatus.NORMAL.withReason("Administrative disconnect"))
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

    fun getConnectedTablets(): List<UUID> {
        return tabletConnections.keys.filterNotNull().toList()
    }

    fun getTabletConnectionInfo(tabletId: UUID): Map<String, Any>? {
        if (tabletId == null) {
            return null
        }

        val connection = tabletConnections[tabletId]
        return if (connection != null) {
            mapOf(
                "deviceId" to tabletId,
                "companyId" to (connection.companyId ?: "unknown"),
                "locationId" to (connection.locationId ?: "unknown"),
                "isAuthenticated" to connection.authenticated,
                "connectedAt" to connection.connectedAt,
                "lastHeartbeat" to connection.lastHeartbeat,
                "tabletName" to (connection.tablet?.friendlyName ?: "unknown"),
                "sessionOpen" to connection.session.isOpen
            )
        } else null
    }

    fun getAllTabletsStatus(): Map<UUID, Map<String, Any>> {
        return tabletConnections.filterKeys { it != null }.mapValues { (_, connection) ->
            mapOf(
                "isOnline" to connection.session.isOpen,
                "isAuthenticated" to connection.authenticated,
                "connectedAt" to connection.connectedAt,
                "lastHeartbeat" to connection.lastHeartbeat,
                "tabletName" to (connection.tablet?.friendlyName ?: "unknown"),
                "companyId" to (connection.companyId ?: "unknown")
            )
        }
    }

    fun sendAdminMessage(tabletId: UUID, messageType: String, data: Map<String, Any>): Boolean {
        if (tabletId == null) {
            logger.warn("Cannot send admin message: tabletId is null")
            return false
        }

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

    fun pingTablet(tabletId: UUID): Boolean {
        return sendAdminMessage(tabletId, "ping", mapOf(
            "requestId" to UUID.randomUUID().toString(),
            "timestamp" to Instant.now()
        ))
    }

    fun requestTabletStatus(tabletId: UUID): Boolean {
        return sendAdminMessage(tabletId, "status_request", mapOf(
            "requestId" to UUID.randomUUID().toString(),
            "requestedFields" to listOf("battery", "orientation", "performance"),
            "timestamp" to Instant.now()
        ))
    }

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

    fun broadcastToWorkstations(companyId: Long, notification: Map<String, Any>): Int {
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

    fun notifySessionCancellation(sessionId: UUID) {
        val message = mapOf(
            "type" to "session_cancelled",
            "payload" to mapOf(
                "sessionId" to sessionId.toString(),
                "timestamp" to Instant.now(),
                "reason" to "Session was cancelled by administrator"
            )
        )

        broadcastToTablets("session_cancelled", message["payload"] as Map<String, Any>)

        logger.info("Session cancellation notification sent for session $sessionId")
    }

    fun sendDocumentSignatureRequestWithDocument(
        tabletId: UUID,
        request: DocumentSignatureRequestDto,
        documentBytes: ByteArray
    ): Boolean {
        val connection = tabletConnections[tabletId]

        return if (connection != null && connection.session.isOpen && connection.authenticated) {

            val documentBase64 = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(documentBytes)

            if (documentBytes.size > 10 * 1024 * 1024) {
                logger.error("Document too large for WebSocket transmission: ${documentBytes.size} bytes")
                return false
            }

            val documentHash = MessageDigest.getInstance("SHA-256")
                .digest(documentBytes)
                .let { hash -> Base64.getEncoder().encodeToString(hash) }

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
                    "instructions" to request.instructions,
                    "businessContext" to request.businessContext,
                    "timeoutMinutes" to request.timeoutMinutes,
                    "expiresAt" to request.expiresAt,
                    "signatureFields" to request.signatureFields,
                    "timestamp" to Instant.now(),
                    "documentData" to documentBase64,
                    "documentSize" to documentBytes.size,
                    "documentHash" to documentHash
                )
            )

            val success = this.sendToSession(connection.session, message)

            if (success) {
                logger.info("Document signature request with embedded document sent to tablet $tabletId for session ${request.sessionId} (${documentBytes.size} bytes)")
            } else {
                logger.warn("Failed to send document signature request with document to tablet $tabletId")
            }
            success
        } else {
            logger.warn("Tablet $tabletId not connected, not authenticated, or session closed")
            false
        }
    }

    fun getConnectionStatistics(): Map<String, Any> {
        val now = Instant.now()
        val staleThreshold = Duration.ofMinutes(2)

        val totalConnections = tabletConnections.size
        val authenticatedConnections = tabletConnections.values.count { it.authenticated }
        val activeConnections = tabletConnections.values.count {
            it.session.isOpen && Duration.between(it.lastHeartbeat, now) < staleThreshold
        }

        val companyStats = tabletConnections.values
            .filter { it.authenticated }
            .groupBy { it.companyId }
            .mapValues { (_, connections) -> connections.size }

        return mapOf(
            "totalConnections" to totalConnections,
            "authenticatedConnections" to authenticatedConnections,
            "activeConnections" to activeConnections,
            "staleConnections" to (totalConnections - activeConnections),
            "companyStats" to companyStats,
            "timestamp" to now
        )
    }
}

data class TabletConnection(
    val session: WebSocketSession,
    val tablet: TabletDevice?,
    val companyId: Long?,
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
    val companyId: Long,
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