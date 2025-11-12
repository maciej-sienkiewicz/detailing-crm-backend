package com.carslab.crm.production.modules.reservations.application.service.query

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.reservations.application.dto.ReservationCountersResponse
import com.carslab.crm.production.modules.reservations.application.dto.ReservationResponse
import com.carslab.crm.production.modules.reservations.application.dto.ReservationServiceResponse
import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.carslab.crm.production.shared.presentation.mapper.DiscountMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class ReservationQueryService(
    private val reservationRepository: ReservationRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ReservationQueryService::class.java)
    private val VAT_RATE = 23

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

        val confirmed = reservationRepository.countByStatus(companyId, ReservationStatus.CONFIRMED)

        return ReservationCountersResponse(
            confirmed = confirmed
        )
    }

    private fun mapToResponse(reservation: Reservation): ReservationResponse {
        val totalNetto = reservation.services.fold(BigDecimal.ZERO) { acc, service ->
            acc.add(service.getTotalNetto())
        }
        val totalBrutto = reservation.services.fold(BigDecimal.ZERO) { acc, service ->
            acc.add(service.getTotalBrutto())
        }
        val totalTax = reservation.services.fold(BigDecimal.ZERO) { acc, service ->
            acc.add(service.getTotalTax())
        }

        return ReservationResponse(
            id = reservation.id.toString(),
            title = reservation.title,
            contactPhone = reservation.contactPhone,
            contactName = reservation.contactName,
            vehicleMake = reservation.vehicleInfo.make,
            vehicleModel = reservation.vehicleInfo.model,
            vehicleDisplay = reservation.vehicleInfo.displayName(),
            startDate = reservation.period.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            endDate = reservation.period.endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            status = reservation.status.name,
            notes = reservation.notes,
            calendarColorId = reservation.calendarColorId,
            visitId = reservation.visitId,
            canBeConverted = reservation.canBeConverted(),
            services = reservation.services.map { mapServiceToResponse(it) },
            serviceCount = reservation.serviceCount(),
            totalPriceNetto = totalNetto,
            totalPriceBrutto = totalBrutto,
            totalTaxAmount = totalTax,
            createdAt = reservation.createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            updatedAt = reservation.updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
        )
    }

    private fun mapServiceToResponse(service: ReservationService): ReservationServiceResponse {
        val unitPrice = service.calculateUnitPrice()
        val finalPrice = service.calculateFinalPrice()

        val discountResponse = service.discount?.let { discount ->
            DiscountMapper.toResponseDto(discount, service.basePrice, VAT_RATE)
        }

        return ReservationServiceResponse(
            id = service.id,
            name = service.name,
            basePrice = PriceResponseDto.from(service.basePrice),
            quantity = service.quantity,
            unitPrice = PriceResponseDto.from(unitPrice),
            finalPrice = PriceResponseDto.from(finalPrice),
            discount = discountResponse,
            note = service.note
        )
    }
}