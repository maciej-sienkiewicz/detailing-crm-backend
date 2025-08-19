package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.production.modules.visits.application.converter.VisitFilterConverter
import com.carslab.crm.production.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitListQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit List Operations", description = "List and detailed view operations for visits")
class VisitListController(
    private val visitListQueryService: VisitListQueryService,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val visitFilterConverter: VisitFilterConverter
) {

    @GetMapping("/list")
    @Operation(summary = "Get visits with pagination and filtering")
    fun getVisits(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") sortDirection: String,
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
        @Parameter(description = "Maximum total price") @RequestParam(required = false) max_price: BigDecimal?
    ): ResponseEntity<PaginatedResponse<VisitListReadModel>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val filter = visitFilterConverter.convertFromRequestParams(
            clientId = null,
            clientName = client_name,
            licensePlate = license_plate,
            status = status,
            startDate = start_date,
            endDate = end_date,
            make = make,
            model = model,
            serviceName = serviceName,
            serviceIds = service_ids,
            title = title,
            minPrice = min_price,
            maxPrice = max_price
        )

        val visits = if (filter.hasAnyFilter()) {
            visitListQueryService.getVisitList(filter, pageable)
        } else {
            visitListQueryService.getVisitList(pageable)
        }

        return ResponseEntity.ok(visits)
    }

    @GetMapping("/{visitId}/detail")
    @Operation(summary = "Get detailed visit information")
    fun getVisitDetail(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(visit)
    }
}