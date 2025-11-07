package com.carslab.crm.production.modules.reservations.api

import com.carslab.crm.production.modules.reservations.application.dto.AddServicesToReservationRequest
import com.carslab.crm.production.modules.reservations.application.dto.RemoveServiceFromReservationRequest
import com.carslab.crm.production.modules.reservations.application.dto.ReservationResponse
import com.carslab.crm.production.modules.reservations.application.dto.UpdateReservationServicesRequest
import com.carslab.crm.production.modules.reservations.application.service.command.ReservationServiceCommandService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation Services", description = "Service management for reservations")
class ReservationServiceController(
    private val serviceCommandService: ReservationServiceCommandService
) {

    @PostMapping("/{reservationId}/services")
    @Operation(
        summary = "Add services to reservation",
        description = "Adds new services to an existing reservation"
    )
    fun addServices(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: AddServicesToReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = serviceCommandService.addServices(reservationId, request)
        return ResponseEntity.ok(reservation)
    }

    @DeleteMapping("/{reservationId}/services")
    @Operation(
        summary = "Remove service from reservation",
        description = "Removes a specific service from the reservation"
    )
    fun removeService(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: RemoveServiceFromReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = serviceCommandService.removeService(reservationId, request)
        return ResponseEntity.ok(reservation)
    }

    @PutMapping("/{reservationId}/services")
    @Operation(
        summary = "Update reservation services",
        description = "Replaces all services in the reservation with the new list"
    )
    fun updateServices(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: UpdateReservationServicesRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = serviceCommandService.updateServices(reservationId, request)
        return ResponseEntity.ok(reservation)
    }
}