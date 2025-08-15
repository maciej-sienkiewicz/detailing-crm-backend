package com.carslab.crm.modules.activities.infrastructure.persistence.read

import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.activities.infrastructure.persistence.repository.ActivityJpaRepositoryDeprecated
import com.carslab.crm.modules.activities.infrastructure.persistence.entity.ActivityEntityDeprecated
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.cache.annotation.Cacheable
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class JpaActivityReadRepositoryImpl(
    private val activityJpaRepositoryDeprecated: ActivityJpaRepositoryDeprecated
) : ActivityReadRepository {

    override fun findActivities(
        category: ActivityCategory?,
        userId: String?,
        entityType: EntityType?,
        entityId: String?,
        status: ActivityStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        search: String?,
        sortBy: String,
        sortOrder: String,
        page: Int,
        size: Int
    ): PaginatedResponse<ActivityReadModel> {
        val companyId = getCurrentCompanyId()

        val sort = if (sortOrder == "asc") {
            Sort.by(Sort.Direction.ASC, sortBy)
        } else {
            Sort.by(Sort.Direction.DESC, sortBy)
        }

        val pageable = PageRequest.of(page, size, sort)

        val specification = Specification<ActivityEntityDeprecated> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            // Company filter (mandatory)
            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            // Category filter
            category?.let {
                predicates.add(cb.equal(root.get<ActivityCategory>("category"), it))
            }

            // User filter
            userId?.let {
                predicates.add(cb.equal(root.get<String>("userId"), it))
            }

            // Entity type filter
            entityType?.let {
                predicates.add(cb.equal(root.get<EntityType>("entityType"), it))
            }

            // Entity ID filter
            entityId?.let {
                predicates.add(cb.equal(root.get<String>("entityId"), it))
            }

            // Status filter
            status?.let {
                predicates.add(cb.equal(root.get<ActivityStatus>("status"), it))
            }

            // Date range filters
            startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), it))
            }

            endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), it))
            }

            // Search filter
            search?.let { searchTerm ->
                val searchPattern = "%${searchTerm.lowercase()}%"
                val messageSearch = cb.like(cb.lower(root.get("message")), searchPattern)
                val userNameSearch = cb.like(cb.lower(root.get("userName")), searchPattern)
                predicates.add(cb.or(messageSearch, userNameSearch))
            }

            cb.and(*predicates.toTypedArray())
        }

        val activityPage = activityJpaRepositoryDeprecated.findAll(specification, pageable)
        val activities = activityPage.content.map { it.toReadModel() }

        return PaginatedResponse(
            data = activities,
            page = page,
            size = size,
            totalItems = activityPage.totalElements,
            totalPages = activityPage.totalPages.toLong()
        )
    }

    override fun findById(activityId: String): ActivityReadModel? {
        val companyId = getCurrentCompanyId()
        return activityJpaRepositoryDeprecated.findByActivityIdAndCompanyId(activityId, companyId)?.toReadModel()
    }

    override fun findRecentActivities(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        limit: Int
    ): List<ActivityReadModel> {
        val companyId = getCurrentCompanyId()
        val pageable = PageRequest.of(0, limit)

        return activityJpaRepositoryDeprecated.findRecentActivities(companyId, startDate, endDate, pageable)
            .map { it.toReadModel() }
    }

    override fun findByEntity(
        entityType: EntityType,
        entityId: String,
        page: Int,
        size: Int
    ): PaginatedResponse<ActivityReadModel> {
        val companyId = getCurrentCompanyId()
        val pageable = PageRequest.of(page, size)

        val activityPage = activityJpaRepositoryDeprecated.findByCompanyIdAndEntityTypeAndEntityIdOrderByTimestampDesc(
            companyId, entityType, entityId, pageable
        )

        val activities = activityPage.content.map { it.toReadModel() }

        return PaginatedResponse(
            data = activities,
            page = page,
            size = size,
            totalItems = activityPage.totalElements,
            totalPages = activityPage.totalPages.toLong()
        )
    }

    override fun findActivitiesForExport(
        category: ActivityCategory?,
        userId: String?,
        entityType: EntityType?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<ActivityReadModel> {
        val companyId = getCurrentCompanyId()

        val specification = Specification<ActivityEntityDeprecated> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            category?.let {
                predicates.add(cb.equal(root.get<ActivityCategory>("category"), it))
            }

            userId?.let {
                predicates.add(cb.equal(root.get<String>("userId"), it))
            }

            entityType?.let {
                predicates.add(cb.equal(root.get<EntityType>("entityType"), it))
            }

            startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), it))
            }

            endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), it))
            }

            cb.and(*predicates.toTypedArray())
        }

        val sort = Sort.by(Sort.Direction.DESC, "timestamp")
        return activityJpaRepositoryDeprecated.findAll(specification, sort).map { it.toReadModel() }
    }

    @Cacheable(value = ["activity-daily-summary"], key = "#date + ':' + @securityContext.getCurrentCompanyId()")
    override fun getDailySummary(date: LocalDate): DailySummaryReadModel {
        val companyId = getCurrentCompanyId()

        val summaryData = activityJpaRepositoryDeprecated.getDailySummary(companyId, date)?.firstOrNull()

        return if (summaryData != null) {
            DailySummaryReadModel(
                date = date,
                appointmentsScheduled = (summaryData[1] as? Number)?.toInt() ?: 0,
                protocolsCompleted = (summaryData[2] as? Number)?.toInt() ?: 0,
                vehiclesServiced = (summaryData[3] as? Number)?.toInt() ?: 0,
                newClients = (summaryData[4] as? Number)?.toInt() ?: 0,
                commentsAdded = (summaryData[5] as? Number)?.toInt() ?: 0,
                totalActivities = (summaryData[6] as? Number)?.toInt() ?: 0
            )
        } else {
            DailySummaryReadModel(
                date = date,
                appointmentsScheduled = 0,
                protocolsCompleted = 0,
                vehiclesServiced = 0,
                newClients = 0,
                commentsAdded = 0,
                totalActivities = 0
            )
        }
    }

    override fun getAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        groupBy: String,
        categories: List<ActivityCategory>?,
        userIds: List<String>?
    ): ActivityAnalyticsReadModel {
        val companyId = getCurrentCompanyId()

        // Get category breakdown
        val categoryBreakdown = activityJpaRepositoryDeprecated.getCategoryBreakdown(companyId, startDate, endDate)
            .associate { row ->
                ActivityCategory.valueOf(row[0] as String) to (row[1] as Number).toInt()
            }

        // Get user breakdown
        val userBreakdown = activityJpaRepositoryDeprecated.getUserBreakdown(companyId, startDate, endDate)
            .associate { row ->
                (row[0] as String) to (row[2] as Number).toInt()
            }

        // Get status breakdown
        val statusBreakdown = activityJpaRepositoryDeprecated.getStatusBreakdown(companyId, startDate, endDate)
            .associate { row ->
                (row[0] as String) to (row[1] as Number).toInt()
            }

        // Get daily trends
        val trendsData = activityJpaRepositoryDeprecated.getDailyTrends(companyId, startDate, endDate)
            .groupBy { row -> row[0] as LocalDate }
            .map { (date, trends) ->
                val categoryCounts = trends.associate { trend ->
                    ActivityCategory.valueOf(trend[1] as String) to (trend[2] as Number).toInt()
                }
                TrendDataPointReadModel(
                    date = date,
                    count = categoryCounts.values.sum(),
                    categories = categoryCounts
                )
            }
            .sortedBy { it.date }

        // Get top users
        val topUsers = activityJpaRepositoryDeprecated.getUserBreakdown(companyId, startDate, endDate)
            .take(10)
            .map { row ->
                TopUserReadModel(
                    userId = row[0] as String,
                    userName = row[1] as String,
                    activityCount = (row[2] as Number).toInt(),
                    categories = emptyMap() // TODO: Implement category breakdown per user
                )
            }

        // Get entity stats
        val entityStats = activityJpaRepositoryDeprecated.getEntityStats(companyId, startDate, endDate)
            .take(20)
            .map { row ->
                EntityStatsReadModel(
                    entityType = EntityType.valueOf(row[0] as String),
                    entityId = row[1] as String,
                    entityDisplayName = "Entity ${row[1]}", // TODO: Get actual display name
                    activityCount = (row[2] as Number).toInt(),
                    lastActivity = row[3] as LocalDateTime
                )
            }

        // Generate daily summaries for the period
        val dailySummaries = generateDailySummariesForPeriod(startDate.toLocalDate(), endDate.toLocalDate())

        val summary = ActivityAnalyticsSummaryReadModel(
            totalActivities = categoryBreakdown.values.sum(),
            categoriesBreakdown = categoryBreakdown,
            usersBreakdown = userBreakdown,
            statusBreakdown = statusBreakdown,
            trendsData = trendsData
        )

        return ActivityAnalyticsReadModel(
            summary = summary,
            dailySummaries = dailySummaries,
            topUsers = topUsers,
            entityStats = entityStats
        )
    }

    private fun generateDailySummariesForPeriod(startDate: LocalDate, endDate: LocalDate): List<DailySummaryReadModel> {
        val summaries = mutableListOf<DailySummaryReadModel>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            summaries.add(getDailySummary(currentDate))
            currentDate = currentDate.plusDays(1)
        }

        return summaries
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}