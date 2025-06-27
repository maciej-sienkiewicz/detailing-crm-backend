// src/main/kotlin/com/carslab/crm/modules/activities/application/queries/models/ActivityQueries.kt
package com.carslab.crm.modules.activities.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.api.model.response.PaginatedResponse
import java.time.LocalDateTime

/**
 * Domain enums matching frontend types
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

enum class ActivityStatus {
    SUCCESS,
    PENDING,
    ERROR
}

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
 * Get paginated activities with filtering
 */
data class GetActivitiesQuery(
    val category: ActivityCategory? = null,
    val userId: String? = null,
    val entityType: EntityType? = null,
    val entityId: String? = null,
    val status: ActivityStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val search: String? = null,
    val sortBy: String = "timestamp",
    val sortOrder: String = "desc",
    val page: Int = 0,
    val size: Int = 20
) : Query<PaginatedResponse<ActivityReadModel>>

/**
 * Get single activity by ID
 */
data class GetActivityByIdQuery(
    val activityId: String
) : Query<ActivityReadModel?>

/**
 * Get activity analytics
 */
data class GetActivityAnalyticsQuery(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val groupBy: String = "day",
    val categories: List<ActivityCategory>? = null,
    val userIds: List<String>? = null
) : Query<ActivityAnalyticsReadModel>

/**
 * Get daily summary
 */
data class GetDailySummaryQuery(
    val date: java.time.LocalDate
) : Query<DailySummaryReadModel>

/**
 * Get recent activities
 */
data class GetRecentActivitiesQuery(
    val limit: Int = 50
) : Query<List<ActivityReadModel>>

/**
 * Get activities by entity
 */
data class GetActivitiesByEntityQuery(
    val entityType: EntityType,
    val entityId: String,
    val page: Int = 0,
    val size: Int = 20
) : Query<PaginatedResponse<ActivityReadModel>>

/**
 * Export activities
 */
data class ExportActivitiesQuery(
    val format: String,
    val category: ActivityCategory? = null,
    val userId: String? = null,
    val entityType: EntityType? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null
) : Query<ByteArray>