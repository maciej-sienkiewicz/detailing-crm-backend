package com.carslab.crm.production.modules.stats.presentation

import com.carslab.crm.production.modules.stats.application.dto.AddToCategoryRequest
import com.carslab.crm.production.modules.stats.application.dto.CategoryResponse
import com.carslab.crm.production.modules.stats.application.dto.CreateCategoryRequest
import com.carslab.crm.production.modules.stats.application.dto.UncategorizedServiceResponse
import com.carslab.crm.production.modules.stats.application.service.StatsCommandService
import com.carslab.crm.production.modules.stats.application.service.StatsQueryService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Statistics management endpoints")
class StatsController(
    private val statsCommandService: StatsCommandService,
    private val statsQueryService: StatsQueryService
) : BaseController() {

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
}