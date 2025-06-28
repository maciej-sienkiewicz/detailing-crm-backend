// src/main/kotlin/com/carslab/crm/modules/activities/api/dto/ActivityDtos.kt
package com.carslab.crm.modules.activities.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Activity category enumeration
 */
enum class ActivityCategory {
    APPOINTMENT,  // Wizyty/rezerwacje
    PROTOCOL,     // Protokoły przyjęcia pojazdów
    COMMENT,      // Komentarze i notatki
    CLIENT,       // Akcje związane z klientami
    VEHICLE,      // Akcje związane z pojazdami
    NOTIFICATION, // Powiadomienia systemowe
    SYSTEM        // Działania systemowe
}

/**
 * Activity status enumeration
 */
enum class ActivityStatus {
    SUCCESS,
    PENDING,
    ERROR
}

/**
 * Entity type enumeration
 */
enum class EntityType {
    APPOINTMENT,  // Wizyta/rezerwacja
    PROTOCOL,     // Protokół przyjęcia
    CLIENT,       // Klient
    VEHICLE,      // Pojazd
    INVOICE,      // Faktura
    COMMENT,      // Komentarz
    SERVICE       // Usługa detailingowa
}

/**
 * Activity entity DTO
 */
data class ActivityEntityDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("type")
    val type: EntityType,

    @JsonProperty("display_name")
    val displayName: String,

    @JsonProperty("related_id")
    val relatedId: String? = null,

    @JsonProperty("metadata")
    val metadata: Map<String, Any>? = null
)

/**
 * Activity metadata DTO
 */
data class ActivityMetadataDto(
    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("previous_value")
    val previousValue: String? = null,

    @JsonProperty("new_value")
    val newValue: String? = null,

    @JsonProperty("appointment_duration")
    val appointmentDuration: Int? = null,

    @JsonProperty("services_list")
    val servicesList: List<String>? = null,

    @JsonProperty("vehicle_condition")
    val vehicleCondition: String? = null,

    @JsonProperty("damage_count")
    val damageCount: Int? = null,

    @JsonProperty("comment_type")
    val commentType: String? = null,

    @JsonProperty("is_resolved")
    val isResolved: Boolean? = null,

    @JsonProperty("notification_type")
    val notificationType: String? = null,

    @JsonProperty("notification_content")
    val notificationContent: String? = null,

    @JsonProperty("is_read")
    val isRead: Boolean? = null,

    @JsonProperty("system_action")
    val systemAction: String? = null,

    @JsonProperty("affected_records")
    val affectedRecords: Int? = null
)

/**
 * Activity detail DTO (for single activity view)
 */
data class ActivityDetailDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("timestamp")
    val timestamp: String,

    @JsonProperty("category")
    val category: ActivityCategory,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("user_id")
    val userId: String? = null,

    @JsonProperty("user_name")
    val userName: String? = null,

    @JsonProperty("user_color")
    val userColor: String? = null,

    @JsonProperty("entity_type")
    val entityType: EntityType? = null,

    @JsonProperty("entity_id")
    val entityId: String? = null,

    @JsonProperty("entities")
    val entities: List<ActivityEntityDto>? = null,

    @JsonProperty("status")
    val status: ActivityStatus? = null,

    @JsonProperty("status_text")
    val statusText: String? = null,

    @JsonProperty("metadata")
    val metadata: ActivityMetadataDto? = null
)

/**
 * Daily summary DTO
 */
data class DailySummaryDto(
    @JsonProperty("date")
    val date: String,

    @JsonProperty("appointments_scheduled")
    val appointmentsScheduled: Int,

    @JsonProperty("protocols_completed")
    val protocolsCompleted: Int,

    @JsonProperty("vehicles_serviced")
    val vehiclesServiced: Int,

    @JsonProperty("new_clients")
    val newClients: Int,

    @JsonProperty("comments_added")
    val commentsAdded: Int,

    @JsonProperty("total_activities")
    val totalActivities: Int
)

/**
 * Activity analytics summary DTO
 */
data class ActivityAnalyticsSummaryDto(
    @JsonProperty("total_activities")
    val totalActivities: Int,

    @JsonProperty("categories_breakdown")
    val categoriesBreakdown: Map<ActivityCategory, Int>,

    @JsonProperty("users_breakdown")
    val usersBreakdown: Map<String, Int>,

    @JsonProperty("status_breakdown")
    val statusBreakdown: Map<String, Int>,

    @JsonProperty("trends_data")
    val trendsData: List<TrendDataPointDto>
)

/**
 * Trend data point DTO
 */
data class TrendDataPointDto(
    @JsonProperty("date")
    val date: String,

    @JsonProperty("count")
    val count: Int,

    @JsonProperty("categories")
    val categories: Map<ActivityCategory, Int>
)

/**
 * Top user DTO
 */
data class TopUserDto(
    @JsonProperty("user_id")
    val userId: String,

    @JsonProperty("user_name")
    val userName: String,

    @JsonProperty("activity_count")
    val activityCount: Int,

    @JsonProperty("categories")
    val categories: Map<ActivityCategory, Int>
)

/**
 * Entity stats DTO
 */
data class EntityStatsDto(
    @JsonProperty("entity_type")
    val entityType: EntityType,

    @JsonProperty("entity_id")
    val entityId: String,

    @JsonProperty("entity_display_name")
    val entityDisplayName: String,

    @JsonProperty("activity_count")
    val activityCount: Int,

    @JsonProperty("last_activity")
    val lastActivity: String
)

/**
 * Complete activity analytics DTO
 */
data class ActivityAnalyticsDto(
    @JsonProperty("summary")
    val summary: ActivityAnalyticsSummaryDto,

    @JsonProperty("daily_summaries")
    val dailySummaries: List<DailySummaryDto>,

    @JsonProperty("top_users")
    val topUsers: List<TopUserDto>,

    @JsonProperty("entity_stats")
    val entityStats: List<EntityStatsDto>
)