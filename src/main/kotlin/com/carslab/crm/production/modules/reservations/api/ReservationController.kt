package com.carslab.crm.production.modules.reservations.api

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.reservations.application.dto.*
import com.carslab.crm.production.modules.reservations.application.service.command.ReservationCommandService
import com.carslab.crm.production.modules.reservations.application.service.query.ReservationQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Reservation management - minimal data before visit")
class ReservationController(
    private val commandService: ReservationCommandService,
    private val queryService: ReservationQueryService
) {

    @PostMapping
    @Operation(summary = "Create new reservation", description = "Create reservation with minimal contact data")
    fun createReservation(
        @Valid @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = commandService.createReservation(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation)
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Get reservation by ID")
    fun getReservation(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String
    ): ResponseEntity<ReservationResponse> {
        val reservation = queryService.getReservation(reservationId)
        return ResponseEntity.ok(reservation)
    }

    @GetMapping
    @Operation(summary = "Get all reservations with pagination")
    fun getReservations(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "startDate") sortBy: String,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") sortDirection: String,
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: String?
    ): ResponseEntity<PaginatedResponse<ReservationResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val reservations = if (status != null) {
            queryService.getReservationsByStatus(status, pageable)
        } else {
            queryService.getReservations(pageable)
        }

        return ResponseEntity.ok(reservations)
    }

    @PutMapping("/{reservationId}")
    @Operation(summary = "Update reservation")
    fun updateReservation(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: UpdateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = commandService.updateReservation(reservationId, request)
        return ResponseEntity.ok(reservation)
    }

    @PatchMapping("/{reservationId}/status")
    @Operation(summary = "Change reservation status")
    fun changeStatus(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: ChangeReservationStatusRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = commandService.changeStatus(reservationId, request)
        return ResponseEntity.ok(reservation)
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Delete reservation")
    fun deleteReservation(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String
    ): ResponseEntity<Void> {
        commandService.deleteReservation(reservationId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/counters")
    @Operation(summary = "Get reservation counters by status")
    fun getCounters(): ResponseEntity<ReservationCountersResponse> {
        val counters = queryService.getCounters()
        return ResponseEntity.ok(counters)
    }
}