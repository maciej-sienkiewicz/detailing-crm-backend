package com.carslab.crm.production.modules.reservations.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.reservations.application.dto.*
import com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation
import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.modules.reservations.domain.models.enums.ReservationStatus
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationPeriod
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.VehicleBasicInfo
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.carslab.crm.production.shared.presentation.mapper.PriceMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ReservationCommandService(
    private val reservationRepository: ReservationRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ReservationCommandService::class.java)

    fun createReservation(request: CreateReservationRequest): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating reservation for company: {}", companyId)

        val startDate = LocalDateTime.parse(request.startDate)
        val endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: startDate.plusHours(1)

        val services = request.selectedServices?.map { serviceRequest ->
            val basePrice = PriceValueObject.createFromInput(
                inputValue = serviceRequest.basePrice.inputPrice,
                inputType = PriceMapper.toDomain(serviceRequest.basePrice.inputType),
                vatRate = 23,
            )

            ReservationService(
                id = serviceRequest.serviceId,
                name = serviceRequest.name,
                basePrice = basePrice,
                quantity = serviceRequest.quantity,
                note = serviceRequest.note
            )
        } ?: emptyList()

        val reservation = Reservation(
            id = null,
            companyId = companyId,
            title = request.title.trim(),
            contactPhone = request.contactPhone.trim(),
            contactName = request.contactName?.trim(),
            vehicleInfo = VehicleBasicInfo(
                make = request.vehicleMake.trim(),
                model = request.vehicleModel.trim()
            ),
            period = ReservationPeriod(startDate, endDate),
            status = ReservationStatus.CONFIRMED,
            services = services,
            notes = request.notes?.trim(),
            calendarColorId = request.calendarColorId,
            visitId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val saved = reservationRepository.save(reservation)
        logger.info("Reservation created successfully: {}", saved.id)

        return mapToResponse(saved)
    }

    fun updateReservation(
        reservationId: String,
        request: UpdateReservationRequest
    ): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating reservation: {} for company: {}", reservationId, companyId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        val startDate = LocalDateTime.parse(request.startDate)
        val endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: startDate.plusHours(1)

        val services = request.selectedServices?.map { serviceRequest ->
            val basePrice = PriceValueObject.createFromInput(
                inputValue = serviceRequest.basePrice.inputPrice,
                inputType = PriceMapper.toDomain(serviceRequest.basePrice.inputType),
                vatRate = 23,
            )

            ReservationService(
                id = serviceRequest.serviceId,
                name = serviceRequest.name,
                basePrice = basePrice,
                quantity = serviceRequest.quantity,
                note = serviceRequest.note
            )
        } ?: reservation.services

        val updated = reservation.update(
            title = request.title.trim(),
            contactPhone = request.contactPhone.trim(),
            contactName = request.contactName?.trim(),
            vehicleInfo = VehicleBasicInfo(
                make = request.vehicleMake.trim(),
                model = request.vehicleModel.trim()
            ),
            period = ReservationPeriod(startDate, endDate),
            services = services,
            notes = request.notes?.trim(),
            calendarColorId = request.calendarColorId
        )

        val saved = reservationRepository.save(updated)
        logger.info("Reservation updated successfully: {}", reservationId)

        return mapToResponse(saved)
    }

    fun changeStatus(
        reservationId: String,
        request: ChangeReservationStatusRequest
    ): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Changing status for reservation: {} to {}", reservationId, request.status)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        val newStatus = try {
            ReservationStatus.valueOf(request.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BusinessException("Invalid status: ${request.status}")
        }

        val updated = reservation.updateStatus(newStatus)
        val saved = reservationRepository.save(updated)

        logger.info("Reservation status changed successfully: {}", reservationId)
        return mapToResponse(saved)
    }

    fun deleteReservation(reservationId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting reservation: {} for company: {}", reservationId, companyId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        if (reservation.status == ReservationStatus.CONVERTED) {
            throw BusinessException("Cannot delete converted reservation")
        }

        reservationRepository.deleteById(ReservationId.of(reservationId), companyId)
        logger.info("Reservation deleted successfully: {}", reservationId)
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
            startDate = reservation.period.startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
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
        val finalPrice = service.calculateFinalPrice()
        return ReservationServiceResponse(
            id = service.id,
            name = service.name,
            basePrice = PriceResponseDto.from(service.basePrice),
            quantity = service.quantity,
            finalPrice = PriceResponseDto.from(finalPrice),
            note = service.note
        )
    }
}