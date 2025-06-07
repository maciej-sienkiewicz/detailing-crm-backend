package com.carslab.crm.finances.api.fixedcosts

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.BreakevenAnalysisResponse
import com.carslab.crm.api.model.BreakevenConfigurationRequest
import com.carslab.crm.api.model.BreakevenConfigurationResponse
import com.carslab.crm.api.model.FinancialProjectionsResponse
import com.carslab.crm.finances.domain.BreakevenService
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfiguration
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api/breakeven")
@Tag(name = "Break-even Analysis", description = "API endpoints for break-even analysis and projections")
class BreakevenController(
    private val breakevenService: BreakevenService
) : BaseController() {

    @GetMapping("/analysis")
    @Operation(summary = "Get break-even analysis", description = "Performs break-even analysis for specified period")
    fun getBreakevenAnalysis(
        @Parameter(description = "Analysis period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) period: LocalDate?
    ): ResponseEntity<BreakevenAnalysisResponse> {
        logger.info("Getting break-even analysis for period: {}", period)

        val analysis = breakevenService.getBreakevenAnalysis(period)

        return ok(analysis)
    }

    @PostMapping("/configuration")
    @Operation(summary = "Save break-even configuration", description = "Creates or updates break-even configuration")
    fun saveConfiguration(
        @Parameter(description = "Configuration data", required = true)
        @RequestBody @Valid request: BreakevenConfigurationRequest
    ): ResponseEntity<BreakevenConfigurationResponse> {
        logger.info("Saving break-even configuration: {}", request.name)

        val savedConfiguration = breakevenService.saveConfiguration(request)
        val response = savedConfiguration.toResponse()

        return created(response)
    }

    @GetMapping("/configuration")
    @Operation(summary = "Get active configuration", description = "Retrieves the currently active break-even configuration")
    fun getActiveConfiguration(): ResponseEntity<BreakevenConfigurationResponse> {
        logger.info("Getting active break-even configuration")

        val configuration = breakevenService.getActiveConfiguration()
        val response = configuration.toResponse()

        return ok(response)
    }

    @GetMapping("/projections")
    @Operation(summary = "Get financial projections", description = "Generates financial projections for specified number of months")
    fun getProjections(
        @Parameter(description = "Number of months") @RequestParam(defaultValue = "12") months: Int
    ): ResponseEntity<FinancialProjectionsResponse> {
        logger.info("Getting financial projections for {} months", months)

        val projections = breakevenService.getProjections(months)

        return ok(projections)
    }

    @GetMapping("/quick-analysis")
    @Operation(summary = "Quick break-even calculation", description = "Performs quick break-even calculation with provided parameters")
    fun quickAnalysis(
        @Parameter(description = "Average service price") @RequestParam servicePrice: BigDecimal,
        @Parameter(description = "Margin percentage") @RequestParam marginPercentage: BigDecimal,
        @Parameter(description = "Fixed costs") @RequestParam fixedCosts: BigDecimal,
        @Parameter(description = "Working days per month") @RequestParam(defaultValue = "22") workingDays: Int
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Performing quick break-even analysis")

        val contributionMargin = servicePrice.multiply(marginPercentage).divide(BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP)
        val breakevenServices = if (contributionMargin > BigDecimal.ZERO) {
            fixedCosts.divide(contributionMargin, 0, BigDecimal.ROUND_CEILING).toInt()
        } else 0

        val breakevenRevenue = BigDecimal(breakevenServices).multiply(servicePrice)
        val servicesPerDay = if (workingDays > 0) (breakevenServices + workingDays - 1) / workingDays else 0

        return ok(mapOf(
            "servicePrice" to servicePrice,
            "marginPercentage" to marginPercentage,
            "contributionMargin" to contributionMargin,
            "fixedCosts" to fixedCosts,
            "breakevenServices" to breakevenServices,
            "breakevenRevenue" to breakevenRevenue,
            "servicesPerDay" to servicesPerDay,
            "workingDays" to workingDays,
            "isAchievable" to (servicesPerDay <= 10) // Reasonable assumption
        ))
    }

    // Helper function to convert domain model to response DTO
    private fun BreakevenConfiguration.toResponse(): BreakevenConfigurationResponse {
        return BreakevenConfigurationResponse(
            id = id.value,
            name = name,
            description = description,
            averageServicePrice = averageServicePrice,
            averageMarginPercentage = averageMarginPercentage,
            contributionMargin = calculateContributionMargin(),
            variableCost = calculateVariableCost(),
            workingDaysPerMonth = workingDaysPerMonth,
            targetServicesPerDay = targetServicesPerDay,
            isActive = isActive,
            createdAt = audit.createdAt,
            updatedAt = audit.updatedAt
        )
    }
}