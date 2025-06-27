// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/persistence/read/ActivityReadRepository.kt
package com.carslab.crm.modules.activities.infrastructure.persistence.read

import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.api.model.response.PaginatedResponse
import java.time.LocalDate
import java.time.LocalDateTime

interface ActivityReadRepository {
    fun findActivities(
        category: ActivityCategory? = null,
        userId: String? = null,
        entityType: EntityType? = null,
        entityId: String? = null,
        status: ActivityStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        search: String? = null,
        sortBy: String = "timestamp",
        sortOrder: String = "desc",
        page: Int = 0,
        size: Int = 20
    ): PaginatedResponse<ActivityReadModel>

    fun findById(activityId: String): ActivityReadModel?

    fun findRecentActivities(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        limit: Int = 50
    ): List<ActivityReadModel>

    fun findByEntity(
        entityType: EntityType,
        entityId: String,
        page: Int = 0,
        size: Int = 20
    ): PaginatedResponse<ActivityReadModel>

    fun findActivitiesForExport(
        category: ActivityCategory? = null,
        userId: String? = null,
        entityType: EntityType? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): List<ActivityReadModel>

    fun getDailySummary(date: LocalDate): DailySummaryReadModel

    fun getAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        groupBy: String = "day",
        categories: List<ActivityCategory>? = null,
        userIds: List<String>? = null
    ): ActivityAnalyticsReadModel
}

