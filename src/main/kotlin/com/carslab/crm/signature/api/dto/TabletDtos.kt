package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class TabletRegistrationRequest(
    @JsonProperty("tenant_id")
    val tenantId: UUID,
    @JsonProperty("location_id")
    val locationId: UUID,
    @JsonProperty("workstation_id")
    val workstationId: UUID?
)

data class TabletPairingRequest(
    val code: String,
    val deviceName: String
)

data class PairingCodeResponse(
    val code: String,
    val expiresIn: Int
)

data class TabletCredentials(
    val deviceId: UUID,
    val deviceToken: String,
    val websocketUrl: String
)

data class TabletStatus(
    val id: UUID,
    val name: String,
    val location: String,
    val isOnline: Boolean,
    val lastSeen: Instant,
    val assignedWorkstation: String?
)