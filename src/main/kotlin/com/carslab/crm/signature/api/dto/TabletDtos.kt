package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

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

/**
 * Detailed tablet information for frontend
 */
data class TabletDeviceDto(
    val id: String,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: String,
    @JsonProperty("friendly_name")
    val friendlyName: String,
    @JsonProperty("workstation_id")
    val workstationId: String?,
    val status: String,
    @JsonProperty("is_online")
    val isOnline: Boolean,
    @JsonProperty("last_seen")
    val lastSeen: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("connection_info")
    val connectionInfo: TabletConnectionInfo?
)

data class TabletConnectionInfo(
    @JsonProperty("connected_at")
    val connectedAt: String?,
    @JsonProperty("last_heartbeat")
    val lastHeartbeat: String?,
    @JsonProperty("is_authenticated")
    val isAuthenticated: Boolean,
    @JsonProperty("session_open")
    val sessionOpen: Boolean,
    @JsonProperty("uptime_minutes")
    val uptimeMinutes: Long?
)

/**
 * Response for listing tablets
 */
data class TabletListResponse(
    val success: Boolean,
    val tablets: List<TabletDeviceDto>,
    @JsonProperty("total_count")
    val totalCount: Int,
    @JsonProperty("online_count")
    val onlineCount: Int,
    val timestamp: Instant
)
