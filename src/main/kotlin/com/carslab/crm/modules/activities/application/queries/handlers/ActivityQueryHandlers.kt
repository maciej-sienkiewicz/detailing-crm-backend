// src/main/kotlin/com/carslab/crm/modules/activities/application/queries/handlers/ActivityQueryHandlers.kt
package com.carslab.crm.modules.activities.application.queries.handlers

import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.activities.infrastructure.persistence.read.ActivityReadRepository
import com.carslab.crm.modules.activities.infrastructure.export.ActivityExportService
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GetActivitiesQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetActivitiesQuery, PaginatedResponse<ActivityReadModel>> {

    private val logger = LoggerFactory.getLogger(GetActivitiesQueryHandler::class.java)

    override fun handle(query: GetActivitiesQuery): PaginatedResponse<ActivityReadModel> {
        logger.debug("Fetching activities with filters: category=${query.category}, entityType=${query.entityType}")

        return activityReadRepository.findActivities(
            category = query.category,
            userId = query.userId,
            entityType = query.entityType,
            entityId = query.entityId,
            status = query.status,
            startDate = query.startDate,
            endDate = query.endDate,
            search = query.search,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            page = query.page,
            size = query.size
        )
    }
}

@Service
class GetActivityByIdQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetActivityByIdQuery, ActivityReadModel?> {

    private val logger = LoggerFactory.getLogger(GetActivityByIdQueryHandler::class.java)

    override fun handle(query: GetActivityByIdQuery): ActivityReadModel? {
        logger.debug("Fetching activity by ID: ${query.activityId}")
        return activityReadRepository.findById(query.activityId)
    }
}

@Service
class GetActivityAnalyticsQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetActivityAnalyticsQuery, ActivityAnalyticsReadModel> {

    private val logger = LoggerFactory.getLogger(GetActivityAnalyticsQueryHandler::class.java)

    override fun handle(query: GetActivityAnalyticsQuery): ActivityAnalyticsReadModel {
        logger.debug("Generating activity analytics for period: ${query.startDate} to ${query.endDate}")

        return activityReadRepository.getAnalytics(
            startDate = query.startDate,
            endDate = query.endDate,
            groupBy = query.groupBy,
            categories = query.categories,
            userIds = query.userIds
        )
    }
}

@Service
class GetDailySummaryQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetDailySummaryQuery, DailySummaryReadModel> {

    private val logger = LoggerFactory.getLogger(GetDailySummaryQueryHandler::class.java)

    override fun handle(query: GetDailySummaryQuery): DailySummaryReadModel {
        logger.debug("Generating daily summary for date: ${query.date}")
        return activityReadRepository.getDailySummary(query.date)
    }
}

@Service
class GetRecentActivitiesQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetRecentActivitiesQuery, List<ActivityReadModel>> {

    private val logger = LoggerFactory.getLogger(GetRecentActivitiesQueryHandler::class.java)

    override fun handle(query: GetRecentActivitiesQuery): List<ActivityReadModel> {
        logger.debug("Fetching recent activities, limit: ${query.limit}")

        val endDate = LocalDateTime.now()
        val startDate = endDate.minusDays(7) // Last 7 days

        return activityReadRepository.findRecentActivities(
            startDate = startDate,
            endDate = endDate,
            limit = query.limit
        )
    }
}

@Service
class GetActivitiesByEntityQueryHandler(
    private val activityReadRepository: ActivityReadRepository
) : QueryHandler<GetActivitiesByEntityQuery, PaginatedResponse<ActivityReadModel>> {

    private val logger = LoggerFactory.getLogger(GetActivitiesByEntityQueryHandler::class.java)

    override fun handle(query: GetActivitiesByEntityQuery): PaginatedResponse<ActivityReadModel> {
        logger.debug("Fetching activities for entity: ${query.entityType}/${query.entityId}")

        return activityReadRepository.findByEntity(
            entityType = query.entityType,
            entityId = query.entityId,
            page = query.page,
            size = query.size
        )
    }
}

@Service
class ExportActivitiesQueryHandler(
    private val activityReadRepository: ActivityReadRepository,
    private val activityExportService: ActivityExportService
) : QueryHandler<ExportActivitiesQuery, ByteArray> {

    private val logger = LoggerFactory.getLogger(ExportActivitiesQueryHandler::class.java)

    override fun handle(query: ExportActivitiesQuery): ByteArray {
        logger.info("Exporting activities in format: ${query.format}")

        // Fetch all activities matching criteria (no pagination for export)
        val activities = activityReadRepository.findActivitiesForExport(
            category = query.category,
            userId = query.userId,
            entityType = query.entityType,
            startDate = query.startDate,
            endDate = query.endDate
        )

        return when (query.format.lowercase()) {
            "xlsx" -> activityExportService.exportToExcel(activities)
            "pdf" -> activityExportService.exportToPdf(activities)
            else -> activityExportService.exportToCsv(activities)
        }
    }
}