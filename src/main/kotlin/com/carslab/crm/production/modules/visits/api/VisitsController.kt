package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitCountersQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.application.service.query.VisitQueryService
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
    private val visitCountersQueryService: VisitCountersQueryService
) {

    @PostMapping
    @Operation(summary = "Create new visit")
    fun createVisit(@Valid @RequestBody request: CreateVisitRequest): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.createVisit(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(visit)
    }

    @GetMapping("/{visitId}")
    @Operation(summary = "Get visit by ID")
    fun getVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<CarReceptionDetailDto> {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        return ResponseEntity.ok(visit)
    }

    @PutMapping("/{visitId}")
    @Operation(summary = "Update visit")
    fun updateVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: UpdateCarReceptionCommand
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.updateVisit(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @PatchMapping("/{visitId}/status")
    @Operation(summary = "Change visit status")
    fun changeVisitStatus(
        @Parameter(description = "Visit ID") @PathVariable visitId: String,
        @Valid @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<VisitResponse> {
        val visit = visitCommandService.changeVisitStatus(visitId, request)
        return ResponseEntity.ok(visit)
    }

    @DeleteMapping("/{visitId}")
    @Operation(summary = "Delete visit")
    fun deleteVisit(
        @Parameter(description = "Visit ID") @PathVariable visitId: String
    ): ResponseEntity<Void> {
        visitCommandService.deleteVisit(visitId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/counters")
    @Operation(summary = "Get visit counters by status")
    fun getVisitCounters(): ResponseEntity<VisitCountersResponse> {
        val counters = visitCountersQueryService.getVisitCounters()
        return ResponseEntity.ok(counters)
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Get visits for specific client")
    fun getVisitsForClient(
        @Parameter(description = "Client ID") @PathVariable clientId: String,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<VisitResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val visits = visitQueryService.getVisitsForClient(clientId, pageable)
        return ResponseEntity.ok(visits)
    }

    @GetMapping("/vehicles/{vehicleId}")
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
}