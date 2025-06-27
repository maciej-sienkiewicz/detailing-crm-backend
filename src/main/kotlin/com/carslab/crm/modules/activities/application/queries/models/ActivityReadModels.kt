// src/main/kotlin/com/carslab/crm/modules/activities/application/queries/models/ActivityReadModels.kt
package com.carslab.crm.modules.activities.application.queries.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Main activity read model
 */
data class ActivityReadModel(
    val id: String,
    val timestamp: LocalDateTime,
    val category: ActivityCategory,
    val message: String,
    val userId: String? = null,
    val userName: String? = null,
    val userColor: String? = null,
    val entityType: EntityType? = null,
    val entityId: String? = null,
    val entities: List<ActivityEntityReadModel>? = null,
    val status: ActivityStatus? = null,
    val statusText: String? = null,
    val metadata: ActivityMetadataReadModel? = null,
    val companyId: Long
)

/**
 * Activity entity read model
 */
data class ActivityEntityReadModel(
    val id: String,
    val type: EntityType,
    val displayName: String,
    val relatedId: String? = null,
    val metadata: Map<String, Any>? = null
)

/**
 * Activity metadata read model
 */
data class ActivityMetadataReadModel(
    val notes: String? = null,
    val previousValue: String? = null,
    val newValue: String? = null,
    val appointmentDuration: Int? = null,
    val servicesList: List<String>? = null,
    val vehicleCondition: String? = null,
    val damageCount: Int? = null,
    val commentType: String? = null,
    val isResolved: Boolean? = null,
    val notificationType: String? = null,
    val notificationContent: String? = null,
    val isRead: Boolean? = null,
    val systemAction: String? = null,
    val affectedRecords: Int? = null
)

/**
 * Daily summary read model
 */
data class DailySummaryReadModel(
    val date: LocalDate,
    val appointmentsScheduled: Int,
    val protocolsCompleted: Int,
    val vehiclesServiced: Int,
    val newClients: Int,
    val commentsAdded: Int,
    val totalActivities: Int
)

/**
 * Analytics summary read model
 */
data class ActivityAnalyticsSummaryReadModel(
    val totalActivities: Int,
    val categoriesBreakdown: Map<ActivityCategory, Int>,
    val usersBreakdown: Map<String, Int>,
    val statusBreakdown: Map<String, Int>,
    val trendsData: List<TrendDataPointReadModel>
)

/**
 * Trend data point read model
 */
data class TrendDataPointReadModel(
    val date: LocalDate,
    val count: Int,
    val categories: Map<ActivityCategory, Int>
)

/**
 * Top user read model
 */
data class TopUserReadModel(
    val userId: String,
    val userName: String,
    val activityCount: Int,
    val categories: Map<ActivityCategory, Int>
)

/**
 * Entity stats read model
 */
data class EntityStatsReadModel(
    val entityType: EntityType,
    val entityId: String,
    val entityDisplayName: String,
    val activityCount: Int,
    val lastActivity: LocalDateTime
)

/**
 * Complete analytics read model
 */
data class ActivityAnalyticsReadModel(
    val summary: ActivityAnalyticsSummaryReadModel,
    val dailySummaries: List<DailySummaryReadModel>,
    val topUsers: List<TopUserReadModel>,
    val entityStats: List<EntityStatsReadModel>
)