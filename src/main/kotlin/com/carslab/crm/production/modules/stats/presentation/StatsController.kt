package com.carslab.crm.production.modules.stats.presentation

import com.carslab.crm.production.modules.stats.application.dto.*
import com.carslab.crm.production.modules.stats.application.service.StatsCommandService
import com.carslab.crm.production.modules.stats.application.service.StatsQueryService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Statistics management endpoints")
class StatsController(
    private val statsCommandService: StatsCommandService,
    private val statsQueryService: StatsQueryService
) : BaseController() {

    @GetMapping("/categories")
    @Operation(summary = "Create a new category", description = "Creates a new service category")
    fun createCategory(): ResponseEntity<List<CategoryResponse>> {

        val response = statsQueryService.getCategoriesWithServiceCounts()
        logger.info("Successfully created category with ID: {}", response)

        return created(response)
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category", description = "Creates a new service category")
    fun createCategory(@Valid @RequestBody request: CreateCategoryRequest): ResponseEntity<CategoryResponse> {
        logger.info("Received request to create new category: {}", request.name)

        val response = statsCommandService.createCategory(request)
        logger.info("Successfully created category with ID: {}", response.id)

        return created(response)
    }

    @PostMapping("/categories/{categoryId}/services")
    @Operation(summary = "Add services to category", description = "Adds services to an existing category")
    fun addToCategory(
        @Parameter(description = "Category ID", required = true) @PathVariable categoryId: Long,
        @Valid @RequestBody request: AddToCategoryRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Adding services to category: {}", categoryId)

        statsCommandService.addToCategory(categoryId, request)
        logger.info("Successfully added services to category: {}", categoryId)

        return ok(mapOf("message" to "Services successfully added to category", "categoryId" to categoryId))
    }

    @GetMapping("/services/uncategorized")
    @Operation(summary = "Get uncategorized services", description = "Retrieves services that are not assigned to any category")
    fun getUncategorizedServices(): ResponseEntity<List<UncategorizedServiceResponse>> {
        logger.info("Getting uncategorized services")

        val response = statsQueryService.getUncategorizedServices()
        logger.info("Successfully retrieved {} uncategorized services", response.size)

        return ok(response)
    }

    @GetMapping("/services/{categoryId}")
    @Operation(summary = "Get categorized services", description = "Retrieves services assigned to a specific category")
    fun getCategorizedServices(
        @Parameter(description = "Category ID", required = true) @PathVariable categoryId: Long
    ): ResponseEntity<List<CategorizedServiceResponse>> {
        logger.info("Getting categorized services for category: {}", categoryId)

        val response = statsQueryService.getCategorizedServices(categoryId)
        logger.info("Successfully retrieved {} categorized services", response.size)

        return ok(response)
    }

    @GetMapping("/categories/{categoryId}/summary")
    @Operation(summary = "Get category statistics summary", description = "Retrieves total orders and revenue for a category")
    fun getCategoryStatsSummary(
        @Parameter(description = "Category ID", required = true) @PathVariable categoryId: Long
    ): ResponseEntity<CategoryStatsSummaryResponse> {
        logger.info("Getting category stats summary for category: {}", categoryId)

        val response = statsQueryService.getCategoryStatsSummary(categoryId)
        logger.info("Successfully retrieved summary for category: {}", categoryId)

        return ok(response)
    }

    @GetMapping("/categories/{category_id}/timeseries")
    @Operation(summary = "Get category time series statistics", description = "Retrieves category statistics over time with specified granularity")
    fun getCategoryStatsTimeSeries(
        @Parameter(description = "Category ID", required = true) @PathVariable category_id: Long,
        @Parameter(description = "Start date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start_date: LocalDate,
        @Parameter(description = "End date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end_date: LocalDate,
        @Parameter(description = "Time granularity", required = true) @RequestParam granularity: TimeGranularity
    ): ResponseEntity<CategoryStatsTimeSeriesResponse> {
        logger.info("Getting category time series stats for category: {} from {} to {} with granularity: {}",
            category_id, start_date, end_date, granularity)

        val request = CategoryStatsRequest(start_date, end_date, granularity)
        val response = statsQueryService.getCategoryStatsTimeSeries(category_id, request)

        logger.info("Successfully retrieved {} time series data points for category: {}",
            response.data.size, category_id)

        return ok(response)
    }

    @GetMapping("/services/{serviceId}/timeseries")
    @Operation(summary = "Get service time series statistics", description = "Retrieves service statistics over time with specified granularity")
    fun getServiceStats(
        @Parameter(description = "Service ID", required = true) @PathVariable serviceId: String,
        @Parameter(description = "Start date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start_date: LocalDate,
        @Parameter(description = "End date", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end_date: LocalDate,
        @Parameter(description = "Time granularity", required = true) @RequestParam granularity: TimeGranularity
    ): ResponseEntity<ServiceStatsResponse> {
        logger.info("Getting service stats for service: {} from {} to {} with granularity: {}",
            serviceId, start_date, end_date, granularity)

        val request = ServiceStatsRequest(serviceId, start_date, end_date, granularity)
        val response = statsQueryService.getServiceStats(request)

        logger.info("Successfully retrieved {} time series data points for service: {}",
            response.data.size, serviceId)

        return ok(response)
    }
}