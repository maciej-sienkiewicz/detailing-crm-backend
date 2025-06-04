package com.carslab.crm.signature.api.websocket

import java.time.Instant
import java.util.UUID

sealed class WebSocketMessage(val type: String)

data class SignatureRequestMessage(
    val sessionId: String,
    val companyId: Long,
    val workstationId: UUID,
    val customerName: String,
    val vehicleInfo: VehicleInfoWS,
    val serviceType: String,
    val documentType: String,
    val timestamp: Instant = Instant.now()
) : WebSocketMessage("signature_request")

data class VehicleInfoWS(
    val make: String,
    val model: String,
    val licensePlate: String,
    val vin: String? = null
)

data class SignatureCompletedMessage(
    val sessionId: String,
    val success: Boolean,
    val signedAt: Instant? = null
) : WebSocketMessage("signature_completed")

data class HeartbeatMessage(
    val timestamp: Instant = Instant.now()
) : WebSocketMessage("heartbeat")

data class ConnectionStatusMessage(
    val status: String,
    val timestamp: Instant = Instant.now()
) : WebSocketMessage("connection")

data class ErrorMessage(
    val error: String,
    val details: String? = null
) : WebSocketMessage("error")