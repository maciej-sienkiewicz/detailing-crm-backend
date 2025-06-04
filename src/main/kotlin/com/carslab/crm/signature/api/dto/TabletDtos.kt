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

/**
 * Detailed tablet information for frontend
 */
data class TabletDeviceDto(
    val id: String,
    @JsonProperty("tenant_id")
    val tenantId: String,
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

/**
 * Detailed tablet info response
 */
data class TabletDetailsResponse(
    val success: Boolean,
    val tablet: TabletDeviceDto,
    @JsonProperty("connection_stats")
    val connectionStats: Map<String, Any>,
    val timestamp: Instant
)

/**
 * Standard API response wrapper for consistent response format
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    val timestamp: Instant = Instant.now()
)

/**
 * Paginated response wrapper
 */
data class PaginatedApiResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val pagination: PaginationInfo,
    val message: String? = null,
    val error: String? = null,
    val timestamp: Instant = Instant.now()
)

data class PaginationInfo(
    @JsonProperty("current_page")
    val currentPage: Int,
    @JsonProperty("page_size")
    val pageSize: Int,
    @JsonProperty("total_items")
    val totalItems: Int,
    @JsonProperty("total_pages")
    val totalPages: Int
)

/**
 * Enhanced tablet response that matches frontend expectations
 */
data class TabletResponse(
    val id: String,
    @JsonProperty("tenant_id")
    val tenantId: String,
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
    val createdAt: String
)

/**
 * Session response for frontend
 */
data class SessionResponse(
    val id: String,
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("tenant_id")
    val tenantId: String,
    @JsonProperty("workstation_id")
    val workstationId: String,
    @JsonProperty("customer_name")
    val customerName: String,
    @JsonProperty("customer_email")
    val customerEmail: String?,
    @JsonProperty("customer_phone")
    val customerPhone: String?,
    @JsonProperty("vehicle_info")
    val vehicleInfo: VehicleInfo?,
    @JsonProperty("service_type")
    val serviceType: String,
    @JsonProperty("document_type")
    val documentType: String,
    val status: String,
    @JsonProperty("expires_at")
    val expiresAt: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("signed_at")
    val signedAt: String?,
    @JsonProperty("assigned_tablet_id")
    val assignedTabletId: String?,
    @JsonProperty("signature_image_url")
    val signatureImageUrl: String?,
    val location: String?,
    val notes: String?,
    val metadata: Map<String, Any>?
)

data class VehicleInfo(
    val make: String,
    val model: String,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val vin: String?,
    val year: Int?,
    val color: String?
)

/**
 * Real-time stats response
 */
data class RealtimeStatsResponse(
    @JsonProperty("connected_tablets")
    val connectedTablets: Int,
    @JsonProperty("pending_sessions")
    val pendingSessions: Int,
    @JsonProperty("completed_today")
    val completedToday: Int,
    @JsonProperty("success_rate")
    val successRate: Double
)