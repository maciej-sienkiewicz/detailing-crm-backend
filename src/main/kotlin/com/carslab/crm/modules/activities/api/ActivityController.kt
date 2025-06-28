// src/main/kotlin/com/carslab/crm/modules/activities/api/ActivityController.kt
package com.carslab.crm.modules.activities.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.activities.api.dto.*
import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.QueryBus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activity Management", description = "System activities monitoring and analytics")
class ActivityController(
    private val queryBus: QueryBus
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get paginated activities with filtering and sorting")
    fun getActivities(
        @Parameter(description = "Activity category filter")
        @RequestParam(required = false) category: String?,

        @Parameter(description = "User ID filter")
        @RequestParam(required = false) userId: String?,

        @Parameter(description = "Entity type filter")
        @RequestParam(required = false) entityType: String?,

        @Parameter(description = "Entity ID filter")
        @RequestParam(required = false) entityId: String?,

        @Parameter(description = "Activity status filter")
        @RequestParam(required = false) status: String?,

        @Parameter(description = "Start date filter (YYYY-MM-DD)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,

        @Parameter(description = "End date filter (YYYY-MM-DD)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,

        @Parameter(description = "Search query in message content")
        @RequestParam(required = false) search: String?,

        @Parameter(description = "Sort field (timestamp, category, user)")
        @RequestParam(defaultValue = "timestamp") sortBy: String,

        @Parameter(description = "Sort order (asc, desc)")
        @RequestParam(defaultValue = "desc") sortOrder: String,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedResponse<ActivityDetailDto>> {

        val query = GetActivitiesQuery(
            category = category?.let { com.carslab.crm.modules.activities.application.queries.models.ActivityCategory.valueOf(it.uppercase()) },
            userId = userId,
            entityType = entityType?.let { com.carslab.crm.modules.activities.application.queries.models.EntityType.valueOf(it.uppercase()) },
            entityId = entityId,
            status = status?.let { com.carslab.crm.modules.activities.application.queries.models.ActivityStatus.valueOf(it.uppercase()) },
            startDate = startDate?.atStartOfDay(),
            endDate = endDate?.plusDays(1)?.atStartOfDay()?.minusSeconds(1),
            search = search,
            sortBy = sortBy,
            sortOrder = sortOrder,
            page = page,
            size = size
        )

        val result = queryBus.execute(query)
        val response = PaginatedResponse(
            data = result.data.map { ActivityMapper.toDetailDto(it) },
            page = result.page,
            size = result.size,
            totalItems = result.totalItems,
            totalPages = result.totalPages
        )

        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    fun getActivityById(
        @Parameter(description = "Activity ID", required = true)
        @PathVariable id: String
    ): ResponseEntity<ActivityDetailDto> {

        val query = GetActivityByIdQuery(id)
        val result = queryBus.execute(query)

        return if (result != null) {
            ok(ActivityMapper.toDetailDto(result))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get activity analytics and statistics")
    fun getActivityAnalytics(
        @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,

        @Parameter(description = "End date (YYYY-MM-DD)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,

        @Parameter(description = "Group by period (day, week, month)")
        @RequestParam(defaultValue = "day") groupBy: String,

        @Parameter(description = "Filter by categories")
        @RequestParam(required = false) categories: List<String>?,

        @Parameter(description = "Filter by user IDs")
        @RequestParam(required = false) userIds: List<String>?
    ): ResponseEntity<ActivityAnalyticsDto> {

        val query = GetActivityAnalyticsQuery(
            startDate = startDate.atStartOfDay(),
            endDate = endDate.plusDays(1).atStartOfDay().minusSeconds(1),
            groupBy = groupBy,
            categories = categories?.map { com.carslab.crm.modules.activities.application.queries.models.ActivityCategory.valueOf(it.uppercase()) },
            userIds = userIds
        )

        val result = queryBus.execute(query)
        return ok(ActivityMapper.toAnalyticsDto(result))
    }

    @GetMapping("/summary/daily")
    @Operation(summary = "Get daily summary for specific date")
    fun getDailySummary(
        @Parameter(description = "Date (YYYY-MM-DD)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DailySummaryDto> {

        val query = GetDailySummaryQuery(date)
        val result = queryBus.execute(query)

        return ok(ActivityMapper.toDailySummaryDto(result))
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent activities (optimized for dashboard)")
    fun getRecentActivities(
        @Parameter(description = "Maximum number of activities")
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<ActivityDetailDto>> {

        val query = GetRecentActivitiesQuery(limit)
        val result = queryBus.execute(query)

        return ok(result.map { ActivityMapper.toDetailDto(it) })
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get activities for specific entity")
    fun getActivitiesByEntity(
        @Parameter(description = "Entity type", required = true)
        @PathVariable entityType: String,

        @Parameter(description = "Entity ID", required = true)
        @PathVariable entityId: String,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedResponse<ActivityDetailDto>> {

        val query = GetActivitiesByEntityQuery(
            entityType = com.carslab.crm.modules.activities.application.queries.models.EntityType.valueOf(entityType.uppercase()),
            entityId = entityId,
            page = page,
            size = size
        )

        val result = queryBus.execute(query)
        val response = PaginatedResponse(
            data = result.data.map { ActivityMapper.toDetailDto(it) },
            page = result.page,
            size = result.size,
            totalItems = result.totalItems,
            totalPages = result.totalPages
        )

        return ok(response)
    }

    @GetMapping("/export")
    @Operation(summary = "Export activities to various formats")
    fun exportActivities(
        @Parameter(description = "Export format (csv, xlsx, pdf)")
        @RequestParam(defaultValue = "csv") format: String,

        @Parameter(description = "Activity category filter")
        @RequestParam(required = false) category: String?,

        @Parameter(description = "User ID filter")
        @RequestParam(required = false) userId: String?,

        @Parameter(description = "Entity type filter")
        @RequestParam(required = false) entityType: String?,

        @Parameter(description = "Start date filter (YYYY-MM-DD)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,

        @Parameter(description = "End date filter (YYYY-MM-DD)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<ByteArray> {

        val query = ExportActivitiesQuery(
            format = format,
            category = category?.let { com.carslab.crm.modules.activities.application.queries.models.ActivityCategory.valueOf(it.uppercase()) },
            userId = userId,
            entityType = entityType?.let { com.carslab.crm.modules.activities.application.queries.models.EntityType.valueOf(it.uppercase()) },
            startDate = startDate?.atStartOfDay(),
            endDate = endDate?.plusDays(1)?.atStartOfDay()?.minusSeconds(1)
        )

        val result = queryBus.execute(query)

        val contentType = when (format.lowercase()) {
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pdf" -> "application/pdf"
            else -> "text/csv"
        }

        val fileName = "activities_export_${LocalDate.now()}.$format"

        return ResponseEntity.ok()
            .header("Content-Type", contentType)
            .header("Content-Disposition", "attachment; filename=\"$fileName\"")
            .body(result)
    }
}