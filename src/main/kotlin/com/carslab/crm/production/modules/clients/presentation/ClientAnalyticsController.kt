package com.carslab.crm.production.modules.clients.presentation

import com.carslab.crm.production.modules.clients.application.dto.ClientAnalyticsResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientAnalyticsSummaryResponse
import com.carslab.crm.production.modules.clients.application.dto.CompanyAveragesResponse
import com.carslab.crm.production.modules.clients.application.service.ClientAnalyticsService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/clients/{clientId}/analytics")
@Tag(name = "Client Analytics", description = "Client analytics and statistics endpoints")
class ClientAnalyticsController(
    private val clientAnalyticsService: ClientAnalyticsService
) : BaseController() {

    @GetMapping
    @HttpMonitored(endpoint = "GET_/api/clients/{clientId}/analytics")
    @Operation(
        summary = "Get comprehensive client analytics",
        description = "Returns detailed analytics including revenue trends, seasonality, top services, and comparisons"
    )
    fun getClientAnalytics(
        @Parameter(description = "Client ID", required = true)
        @PathVariable clientId: Long
    ): ResponseEntity<ClientAnalyticsResponse> {
        logger.info("Getting comprehensive analytics for client: {}", clientId)

        val analytics = clientAnalyticsService.getClientAnalytics(ClientId.of(clientId))
            ?: return badRequest("Client analytics not found")

        logger.info("Successfully retrieved analytics for client: {}", clientId)
        return ok(analytics)
    }

    @GetMapping("/summary")
    @HttpMonitored(endpoint = "GET_/api/clients/{clientId}/analytics/summary")
    @Operation(
        summary = "Get client analytics summary",
        description = "Returns lightweight analytics summary for dashboard/list views"
    )
    fun getClientAnalyticsSummary(
        @Parameter(description = "Client ID", required = true)
        @PathVariable clientId: Long
    ): ResponseEntity<ClientAnalyticsSummaryResponse> {
        logger.info("Getting analytics summary for client: {}", clientId)

        val summary = clientAnalyticsService.getClientAnalyticsSummary(ClientId.of(clientId))
            ?: return badRequest("Client analytics summary not found")

        logger.info("Successfully retrieved analytics summary for client: {}", clientId)
        return ok(summary)
    }
}

@RestController
@RequestMapping("/api/company/analytics")
@Tag(name = "Company Analytics", description = "Company-wide analytics endpoints")
class CompanyAnalyticsController(
    private val clientAnalyticsService: ClientAnalyticsService
) : BaseController() {

    @GetMapping("/averages")
    @HttpMonitored(endpoint = "GET_/api/company/analytics/averages")
    @Operation(
        summary = "Get company averages",
        description = "Returns company-wide average metrics for comparison purposes"
    )
    fun getCompanyAverages(): ResponseEntity<CompanyAveragesResponse> {
        logger.info("Getting company averages")

        val averages = clientAnalyticsService.getCompanyAverages()

        logger.info("Successfully retrieved company averages")
        return ok(averages)
    }

    @PostMapping("/clients/batch")
    @HttpMonitored(endpoint = "POST_/api/company/analytics/clients/batch")
    @Operation(
        summary = "Get batch client analytics",
        description = "Returns basic analytics for multiple clients in a single request"
    )
    fun getBatchClientAnalytics(
        @RequestBody clientIds: List<Long>
    ): ResponseEntity<Map<String, ClientAnalyticsResponse>> {
        logger.info("Getting batch analytics for {} clients", clientIds.size)

        if (clientIds.isEmpty()) {
            return badRequest("Client IDs list cannot be empty")
        }

        if (clientIds.size > 100) {
            return badRequest("Maximum 100 clients allowed per batch request")
        }

        val analytics = clientAnalyticsService.getBatchClientAnalytics(
            clientIds.map { ClientId.of(it) }
        )

        val response = analytics.mapKeys { it.key.value.toString() }

        logger.info("Successfully retrieved batch analytics for {} clients", response.size)
        return ok(response)
    }
}