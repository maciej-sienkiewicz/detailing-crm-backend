package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.modules.visits.api.commands.ClientProtocolHistoryDto
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitCountersQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitListQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/protocols")
@Tag(name = "Visit Core Operations", description = "Core CRUD operations for visits")
class VisitController(
    private val visitCommandService: VisitCommandService,
    private val visitQueryService: VisitQueryService,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val visitCountersQueryService: VisitCountersQueryService,
    private val visitListQueryService: VisitListQueryService
) {

    @PostMapping
    @HttpMonitored(endpoint = "POST_/api/v1/protocols")
    @Operation(summary = "Create new visit")
    fun createVisit(@Valid @RequestBody request: CreateVisitRequest): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.createVisit(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(visit)
    }

    @GetMapping("/{visitId}")
    @HttpMonitored(endpoint = "GET_/api/v1/protocols/{id}")
    @Operation(summary = "Get visit by ID")
    fun getVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(visit)
    }

    @PostMapping("/{id}/release")
    @HttpMonitored(endpoint = "POST_/api/v1/protocols/{id}/release")
    @Operation(summary = "Release vehicle to client", description = "Completes protocol by releasing vehicle to client with payment details")
    fun releaseVehicle(
        @Parameter(description = "Visit ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: ReleaseVehicleRequest
    ): ResponseEntity<CarReceptionDetailDto> {
        visitCommandService.release(id, request)
        val read = visitDetailQueryService.getVisitDetail(id)
        return ResponseEntity.ok(read)
    }

    @PutMapping("/{visitId}")
    @HttpMonitored(endpoint = "PUT_/api/v1/protocols/{id}")
    @Operation(summary = "Update visit")
    fun updateVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: UpdateCarReceptionCommand
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitCommandService.updateVisit(visitId, request)
        val read = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(read)
    }

    @PostMapping("/{visitId}/services")
    @HttpMonitored(endpoint = "POST_/api/v1/protocols/{id}/services")
    @Operation(summary = "Add services to visit", description = "Adds new services to an existing visit with PENDING approval status")
    fun addServicesToVisit(
        @Parameter(description = "Visit ID", required = true) @PathVariable visitId: String,
        @Valid @RequestBody request: AddServicesToVisitRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.addServicesToVisit(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @DeleteMapping("/{visitId}/services")
    @HttpMonitored(endpoint = "DELETE_/api/v1/protocols/{id}/services")
    @Operation(summary = "Remove service from visit", description = "Removes a service from an existing visit")
    fun removeServiceFromVisit(
        @Parameter(description = "Visit ID", required = true) @PathVariable visitId: String,
        @Valid @RequestBody request: RemoveServiceFromVisitRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.removeServiceFromVisit(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @PatchMapping("/{visitId}/status")
    @HttpMonitored(endpoint = "PATCH_/api/v1/protocols/{id}/status")
    @Operation(summary = "Change visit status")
    fun changeVisitStatus(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.changeVisitStatus(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @DeleteMapping("/{visitId}")
    @HttpMonitored(endpoint = "DELETE_/api/v1/protocols/{id}")
    @Operation(summary = "Delete visit")
    fun deleteVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<Void> {
        visitCommandService.deleteVisit(visitId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/counters")
    @HttpMonitored(endpoint = "GET_/api/v1/protocols/counters")
    @Operation(summary = "Get visit counters by status")
    fun getVisitCounters(): ResponseEntity<VisitCountersResponse> {
        val counters = visitCountersQueryService.getVisitCounters()
        return ResponseEntity.ok(counters)
    }

    @GetMapping("/client/{clientId}")
    @HttpMonitored(endpoint = "GET_/api/v1/protocols/client/{id}")
    @Operation(summary = "Get visits for specific client")
    fun getVisitsForClient(
        @Parameter(description = "Client ID") @PathVariable clientId: String,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedResponse<ClientProtocolHistoryDto>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val visits = visitListQueryService.getVisitList(VisitListFilterRequest(clientId = clientId), pageable)
        return ResponseEntity.ok(PaginatedResponse(
            data = visits.data.map { ClientProtocolHistoryDto(
                id = it.id,
                startDate = it.period.startDate,
                endDate = it.period.endDate,
                status = ApiProtocolStatus.valueOf(it.status),
                carMake = it.vehicle.make,
                carModel = it.vehicle.model,
                licensePlate = it.vehicle.licensePlate,
                totalAmount = PriceResponseDto(
                    it.totalAmountNetto,
                    it.totalAmountBrutto,
                    it.totalTaxAmount
                ),
                title = it.title
            ) },
            page = visits.page,
            size = visits.size,
            totalItems = visits.totalItems,
            totalPages = visits.totalPages
        ))
    }

    @GetMapping("/vehicles/{vehicleId}")
    @HttpMonitored(endpoint = "GET_/api/v1/protocols/vehicles/{id}")
    @Operation(summary = "Get visits for specific vehicle")
    fun getVisitsForVehicle(
        @Parameter(description = "Vehicle ID") @PathVariable vehicleId: String,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<VisitResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val visits = visitQueryService.getVisitsForVehicle(vehicleId, pageable)
        return ResponseEntity.ok(visits)
    }

    @PutMapping("/{visitId}/services")
    @HttpMonitored(endpoint = "PUT_/api/v1/protocols/{id}/services")
    @Operation(summary = "Update visit services", description = "Updates existing services in a visit by matching service names")
    fun updateVisitServices(
        @Parameter(description = "Visit ID", required = true) @PathVariable visitId: String,
        @Valid @RequestBody request: UpdateVisitServicesRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.updateVisitServices(visitId, request)
        return ResponseEntity.ok(visit)
    }
}