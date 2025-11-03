package com.carslab.crm.production.modules.reservations.application.service.command

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.reservations.application.dto.ConvertReservationToVisitRequest
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommandService
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReservationConversionService(
    private val reservationRepository: ReservationRepository,
    private val visitCommandService: VisitCommandService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ReservationConversionService::class.java)

    fun convertToVisit(
        reservationId: String,
        request: ConvertReservationToVisitRequest
    ): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Converting reservation: {} to visit for company: {}", reservationId, companyId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        if (!reservation.canBeConverted()) {
            throw BusinessException(
                "Reservation cannot be converted. Status: ${reservation.status}, VisitId: ${reservation.visitId}"
            )
        }

        // Utworzenie wizyty z pełnymi danymi
        val createVisitRequest = CreateVisitRequest(
            title = reservation.title,
            calendarColorId = reservation.calendarColorId,
            startDate = reservation.period.startDate.toString(),
            endDate = reservation.period.endDate.toString(),

            // Dane pojazdu
            make = reservation.vehicleInfo.make,
            model = reservation.vehicleInfo.model,
            licensePlate = request.licensePlate,
            productionYear = request.productionYear,
            mileage = request.mileage,
            vin = request.vin,
            color = request.color,

            // Dane klienta
            ownerName = request.ownerName,
            email = request.email,
            phone = reservation.contactPhone,
            companyName = request.companyName,
            taxId = request.taxId,
            address = request.address,

            // Dodatkowe
            keysProvided = request.keysProvided,
            documentsProvided = request.documentsProvided,
            notes = combineNotes(reservation.notes, request.additionalNotes),
            selectedServices = request.selectedServices,
            status = ApiProtocolStatus.IN_PROGRESS,
            referralSource = null,
            appointmentId = null,
            ownerId = null,
            deliveryPerson = null,
            isRecurring = false,
            recurrencePattern = null
        )

        val visit = visitCommandService.createVisit(createVisitRequest)

        // Aktualizacja rezerwacji - oznaczenie jako przekonwertowana
        val updatedReservation = reservation.markAsConverted(visit.id.toLong())
        reservationRepository.save(updatedReservation)

        logger.info(
            "Reservation: {} converted to visit: {} successfully",
            reservationId,
            visit.id
        )

        return visit
    }

    private fun combineNotes(reservationNotes: String?, additionalNotes: String?): String? {
        return when {
            reservationNotes.isNullOrBlank() && additionalNotes.isNullOrBlank() -> null
            reservationNotes.isNullOrBlank() -> additionalNotes
            additionalNotes.isNullOrBlank() -> reservationNotes
            else -> "$reservationNotes\n\nDodatkowe uwagi przy rozpoczęciu:\n$additionalNotes"
        }
    }
}