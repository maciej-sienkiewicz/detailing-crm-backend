package com.carslab.crm.modules.visits.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.domain.model.ProtocolStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Enhanced Visit Search", description = "Optimized visit search with comprehensive filtering")
class EnhancedVisitSearchController(
    private val queryBus: QueryBus
) : BaseController() {

    @GetMapping("/list")
    @Operation(
        summary = "Search visits with enhanced filtering",
        description = "Comprehensive visit search with optimized performance and multi-tenant security"
    )
    fun searchVisits(
        @Parameter(description = "Client name (partial match)") @RequestParam(required = false) client_name: String?,
        @Parameter(description = "License plate (partial match)") @RequestParam(required = false) license_plate: String?,
        @Parameter(description = "Visit status") @RequestParam(required = false) status: String?,
        @Parameter(description = "Start date filter (ISO format)") @RequestParam(required = false) start_date: String?,
        @Parameter(description = "End date filter (ISO format)") @RequestParam(required = false) end_date: String?,
        @Parameter(description = "Vehicle make (partial match)") @RequestParam(required = false) make: String?,
        @Parameter(description = "Vehicle model (partial match)") @RequestParam(required = false) model: String?,
        @Parameter(description = "Service name (partial match)") @RequestParam(required = false) serviceName: String?,
        @Parameter(description = "Service IDs") @RequestParam(required = false) service_ids: List<String>?,
        @Parameter(description = "Visit title (partial match)") @RequestParam(required = false) title: String?,
        @Parameter(description = "Minimum total price") @RequestParam(required = false) min_price: BigDecimal?,
        @Parameter(description = "Maximum total price") @RequestParam(required = false) max_price: BigDecimal?,
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PaginatedResponse<VisitListReadModel>> {

        val query = SearchVisitsQuery(
            clientName = client_name?.takeIf { it.isNotBlank() },
            licensePlate = license_plate?.takeIf { it.isNotBlank() },
            status = status?.takeIf { it.isNotBlank() }?.let { ProtocolStatus.valueOf(it) },
            startDate = parseDateTime(start_date),
            endDate = parseDateTime(end_date),
            make = make?.takeIf { it.isNotBlank() },
            model = model?.takeIf { it.isNotBlank() },
            serviceName = serviceName?.takeIf { it.isNotBlank() },
            serviceIds = service_ids?.takeIf { it.isNotEmpty() },
            title = title?.takeIf { it.isNotBlank() },
            minPrice = min_price,
            maxPrice = max_price,
            page = page.coerceAtLeast(0),
            size = size.coerceIn(1, 100)
        )

        val result = queryBus.execute(query)
        return ok(result)
    }

    private fun parseDateTime(dateTimeString: String?): LocalDateTime? {
        if (dateTimeString.isNullOrBlank()) return null

        return try {
            LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            try {
                val date = java.time.LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_DATE)
                LocalDateTime.of(date, java.time.LocalTime.of(0, 0))
            } catch (e2: Exception) {
                null
            }
        }
    }
}