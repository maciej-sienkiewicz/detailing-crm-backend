package com.carslab.crm.production.modules.reservations.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.reservations.application.dto.*
import com.carslab.crm.production.modules.reservations.domain.models.entities.ReservationService
import com.carslab.crm.production.modules.reservations.domain.models.value_objects.ReservationId
import com.carslab.crm.production.modules.reservations.domain.repositories.ReservationRepository
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.carslab.crm.production.shared.presentation.mapper.PriceMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ReservationServiceCommandService(
    private val reservationRepository: ReservationRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ReservationServiceCommandService::class.java)

    fun addServices(
        reservationId: String,
        request: AddServicesToReservationRequest
    ): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Adding services to reservation: {} for company: {}", reservationId, companyId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        val newServices = request.services.map { serviceRequest ->
            val basePrice = PriceValueObject.createFromInput(
                inputValue = serviceRequest.basePrice.inputPrice,
                inputType = PriceMapper.toDomain(serviceRequest.basePrice.inputType,),
                vatRate = 23
            )

            ReservationService(
                id = serviceRequest.serviceId,
                name = serviceRequest.name,
                basePrice = basePrice,
                quantity = serviceRequest.quantity,
                note = serviceRequest.note
            )
        }

        val currentServices = reservation.services.toMutableList()
        currentServices.addAll(newServices)

        val updated = reservation.updateServices(currentServices)
        val saved = reservationRepository.save(updated)

        logger.info("Added {} services to reservation: {}", newServices.size, reservationId)
        return mapToResponse(saved)
    }

    fun removeService(
        reservationId: String,
        request: RemoveServiceFromReservationRequest
    ): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Removing service: {} from reservation: {}", request.serviceId, reservationId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        val updatedServices = reservation.services.filterNot { it.id == request.serviceId }

        val updated = reservation.updateServices(updatedServices)
        val saved = reservationRepository.save(updated)

        logger.info("Removed service from reservation: {}", reservationId)
        return mapToResponse(saved)
    }

    fun updateServices(
        reservationId: String,
        request: UpdateReservationServicesRequest
    ): ReservationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating services for reservation: {}", reservationId)

        val reservation = reservationRepository.findById(ReservationId.of(reservationId), companyId)
            ?: throw EntityNotFoundException("Reservation not found: $reservationId")

        val updatedServices = request.services.map { serviceRequest ->
            val basePrice = PriceValueObject.createFromInput(
                inputValue = serviceRequest.basePrice.inputPrice,
                inputType = PriceMapper.toDomain(serviceRequest.basePrice.inputType),
                vatRate = 23
            )

            ReservationService(
                id = serviceRequest.serviceId,
                name = serviceRequest.name,
                basePrice = basePrice,
                quantity = serviceRequest.quantity,
                note = serviceRequest.note
            )
        }

        val updated = reservation.updateServices(updatedServices)
        val saved = reservationRepository.save(updated)

        logger.info("Updated services for reservation: {}", reservationId)
        return mapToResponse(saved)
    }

    private fun mapToResponse(reservation: com.carslab.crm.production.modules.reservations.domain.models.aggregates.Reservation): ReservationResponse {
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
            endDate = reservation.period.endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
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