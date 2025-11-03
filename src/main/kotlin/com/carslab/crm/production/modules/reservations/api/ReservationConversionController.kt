package com.carslab.crm.production.modules.reservations.api

import com.carslab.crm.production.modules.reservations.application.dto.ConvertReservationToVisitRequest
import com.carslab.crm.production.modules.reservations.application.service.command.ReservationConversionService
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation Conversion", description = "Convert reservation to visit")
class ReservationConversionController(
    private val conversionService: ReservationConversionService
) {

    @PostMapping("/{reservationId}/convert-to-visit")
    @Operation(
        summary = "Convert reservation to visit",
        description = "Starts the visit by providing full client and vehicle data"
    )
    fun convertToVisit(
        @Parameter(description = "Reservation ID") @PathVariable reservationId: String,
        @Valid @RequestBody request: ConvertReservationToVisitRequest
    ): ResponseEntity<VisitResponse> {
        val visit = conversionService.convertToVisit(reservationId, request)
        return ResponseEntity.ok(visit)
    }
}