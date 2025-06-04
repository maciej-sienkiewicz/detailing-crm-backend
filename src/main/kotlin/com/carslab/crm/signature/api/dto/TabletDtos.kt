package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class TabletRegistrationRequest(
    @field:NotNull(message = "Company ID is required")
    @field:Positive(message = "Company ID must be positive")
    @JsonProperty("company_id")
    val companyId: Long,

    @field:NotNull(message = "Location ID is required")
    @field:Positive(message = "Location ID must be positive")
    @JsonProperty("location_id")
    val locationId: Long,

    @JsonProperty("workstation_id")
    val workstationId: Long? = null,

    @JsonProperty("device_name")
    @field:Size(max = 100, message = "Device name cannot exceed 100 characters")
    val deviceName: String? = null
)

data class TabletPairingRequest(
    @field:NotBlank(message = "Pairing code is required")
    @field:Size(min = 6, max = 10, message = "Pairing code must be between 6 and 10 characters")
    val code: String,

    @field:NotBlank(message = "Device name is required")
    @field:Size(min = 3, max = 100, message = "Device name must be between 3 and 100 characters")
    @JsonProperty("device_name")
    val deviceName: String
)

data class PairingCodeResponse(
    val code: String,
    @JsonProperty("expires_in")
    val expiresIn: Int, // seconds until expiration
    @JsonProperty("expires_at")
    val expiresAt: Instant,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: Long
)

data class TabletCredentials(
    @JsonProperty("device_id")
    val deviceId: Long,
    @JsonProperty("device_token")
    val deviceToken: String,
    @JsonProperty("websocket_url")
    val websocketUrl: String,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: Long
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
    val id: Long,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: Long,
    @JsonProperty("friendly_name")
    val friendlyName: String,
    @JsonProperty("workstation_id")
    val workstationId: Long?,
    @JsonProperty("workstation_name")
    val workstationName: String?,
    val status: String,
    @JsonProperty("is_online")
    val isOnline: Boolean,
    @JsonProperty("last_seen")
    val lastSeen: Instant,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant,
    @JsonProperty("connection_info")
    val connectionInfo: TabletConnectionInfo?
)

data class TabletConnectionInfo(
    @JsonProperty("connected_at")
    val connectedAt: Instant?,
    @JsonProperty("last_heartbeat")
    val lastHeartbeat: Instant?,
    @JsonProperty("is_authenticated")
    val isAuthenticated: Boolean,
    @JsonProperty("session_open")
    val sessionOpen: Boolean,
    @JsonProperty("uptime_minutes")
    val uptimeMinutes: Long?,
    @JsonProperty("signal_strength")
    val signalStrength: String? = null,
    @JsonProperty("battery_level")
    val batteryLevel: Int? = null
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
    @JsonProperty("company_id")
    val companyId: Long,
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
    @JsonProperty("recent_sessions")
    val recentSessions: List<SessionSummaryDto>?,
    val timestamp: Instant
)

data class SessionSummaryDto(
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("customer_name")
    val customerName: String,
    val status: String,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("signed_at")
    val signedAt: Instant?
)

/**
 * Standard API response wrapper for consistent response format
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    @JsonProperty("company_id")
    val companyId: Long? = null,
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
    @JsonProperty("company_id")
    val companyId: Long? = null,
    val timestamp: Instant = Instant.now()
)

data class PaginationInfo(
    @JsonProperty("current_page")
    val currentPage: Int,
    @JsonProperty("page_size")
    val pageSize: Int,
    @JsonProperty("total_items")
    val totalItems: Long,
    @JsonProperty("total_pages")
    val totalPages: Int,
    @JsonProperty("has_next")
    val hasNext: Boolean,
    @JsonProperty("has_previous")
    val hasPrevious: Boolean
)

/**
 * Enhanced tablet response that matches frontend expectations
 */
data class TabletResponse(
    val id: Long,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: Long,
    @JsonProperty("friendly_name")
    val friendlyName: String,
    @JsonProperty("workstation_id")
    val workstationId: Long?,
    val status: String,
    @JsonProperty("is_online")
    val isOnline: Boolean,
    @JsonProperty("last_seen")
    val lastSeen: Instant,
    @JsonProperty("created_at")
    val createdAt: Instant
)

/**
 * Session response for frontend
 */
data class SessionResponse(
    val id: Long,
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("workstation_id")
    val workstationId: Long,
    @JsonProperty("assigned_tablet_id")
    val assignedTabletId: Long?,
    @JsonProperty("customer_name")
    val customerName: String,
    @JsonProperty("customer_email")
    val customerEmail: String?,
    @JsonProperty("customer_phone")
    val customerPhone: String?,
    @JsonProperty("vehicle_info")
    val vehicleInfo: VehicleInfo?,
    @JsonProperty("service_type")
    val serviceType: String?,
    @JsonProperty("document_type")
    val documentType: String?,
    val status: String,
    @JsonProperty("expires_at")
    val expiresAt: Instant,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("signed_at")
    val signedAt: Instant?,
    @JsonProperty("signature_image_url")
    val signatureImageUrl: String?,
    @JsonProperty("additional_notes")
    val additionalNotes: String?,
    @JsonProperty("signature_duration_seconds")
    val signatureDurationSeconds: Int?,
    val metadata: Map<String, Any>?
)

data class VehicleInfo(
    val make: String?,
    val model: String?,
    @JsonProperty("license_plate")
    val licensePlate: String?,
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
    val successRate: Double,
    @JsonProperty("average_completion_time")
    val averageCompletionTime: Double?, // in seconds
    @JsonProperty("company_id")
    val companyId: Long,
    val timestamp: Instant
)

data class TabletStatsResponse(
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("total_tablets")
    val totalTablets: Int,
    @JsonProperty("online_tablets")
    val onlineTablets: Int,
    @JsonProperty("active_tablets")
    val activeTablets: Int,
    @JsonProperty("tablets_by_status")
    val tabletsByStatus: Map<String, Int>,
    @JsonProperty("tablets_by_location")
    val tabletsByLocation: Map<Long, Int>,
    @JsonProperty("sessions_today")
    val sessionsToday: Int,
    @JsonProperty("sessions_this_week")
    val sessionsThisWeek: Int,
    @JsonProperty("sessions_this_month")
    val sessionsThisMonth: Int,
    val timestamp: Instant
)

/**
 * Location information
 */
data class LocationDto(
    val id: Long,
    @JsonProperty("company_id")
    val companyId: Long,
    val name: String,
    val address: String?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("tablet_count")
    val tabletCount: Int,
    @JsonProperty("workstation_count")
    val workstationCount: Int
)

/**
 * Workstation information
 */
data class WorkstationDto(
    val id: Long,
    @JsonProperty("company_id")
    val companyId: Long,
    @JsonProperty("location_id")
    val locationId: Long,
    @JsonProperty("workstation_name")
    val workstationName: String,
    @JsonProperty("workstation_code")
    val workstationCode: String?,
    @JsonProperty("paired_tablet_id")
    val pairedTabletId: Long?,
    @JsonProperty("paired_tablet_name")
    val pairedTabletName: String?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("last_activity")
    val lastActivity: Instant?,
    @JsonProperty("created_at")
    val createdAt: Instant
)

/**
 * Error response format
 */
data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val code: String,
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now()
)

/**
 * Health check response
 */
data class HealthResponse(
    val status: String, // UP, DOWN, DEGRADED
    val timestamp: Instant,
    val details: Map<String, Any> = emptyMap()
)