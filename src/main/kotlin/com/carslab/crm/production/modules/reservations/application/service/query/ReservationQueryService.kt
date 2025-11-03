package com.carslab.crm.production.modules.reservations.application.service.query

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.reservations.application.dto.ReservationCountersResponse
import com.carslab.crm.production.modules.reservations.application.dto.ReservationResponse
import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReservationQueryService(
    private val reservationRepository: ReservationRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ReservationQueryService::class.java)

    fun getReservation(reservationId: String): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching reservation: {} for company: {}", reservationId, companyId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        return mapToResponse(reservation)
    }

    fun getReservations(pageable: Pageable): PaginatedResponse<ReservationResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching reservations for company: {}", companyId)

        val page = reservationRepository.findByCompanyId(companyId, pageable)
        val responses = page.content.map { mapToResponse(it) }

        return PaginatedResponse(
            data = responses,
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
            totalPages = page.totalPages.toLong()
        )
    }

    fun getReservationsByStatus(
        status: String,
        pageable: Pageable
    ): PaginatedResponse<ReservationResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching reservations with status: {} for company: {}", status, companyId)

        val reservationStatus = try {
            ReservationStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $status")
        }

        val page = reservationRepository.findByCompanyIdAndStatus(
            companyId,
            reservationStatus,
            pageable
        )
        val responses = page.content.map { mapToResponse(it) }

        return PaginatedResponse(
            data = responses,
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
            totalPages = page.totalPages.toLong()
        )
    }

    fun getCounters(): ReservationCountersResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching reservation counters for company: {}", companyId)

        val pending = reservationRepository.countByStatus(companyId, ReservationStatus.PENDING)
        val confirmed = reservationRepository.countByStatus(companyId, ReservationStatus.CONFIRMED)
        val converted = reservationRepository.countByStatus(companyId, ReservationStatus.CONVERTED)
        val cancelled = reservationRepository.countByStatus(companyId, ReservationStatus.CANCELLED)

        return ReservationCountersResponse(
            pending = pending,
            confirmed = confirmed,
            converted = converted,
            cancelled = cancelled,
            all = pending + confirmed + converted + cancelled
        )
    }

    private fun mapToResponse(reservation: Reservation): ReservationResponse {
        return ReservationResponse(
            id = reservation.id.toString(),
            title = reservation.title,
            contactPhone = reservation.contactPhone,
            contactName = reservation.contactName,
            vehicleMake = reservation.vehicleInfo.make,
            vehicleModel = reservation.vehicleInfo.model,
            vehicleDisplay = reservation.vehicleInfo.displayName(),
            startDate = reservation.period.startDate,
            endDate = reservation.period.endDate,
            status = reservation.status.name,
            notes = reservation.notes,
            calendarColorId = reservation.calendarColorId,
            visitId = reservation.visitId,
            canBeConverted = reservation.canBeConverted(),
            createdAt = reservation.createdAt,
            updatedAt = reservation.updatedAt
        )
    }
}