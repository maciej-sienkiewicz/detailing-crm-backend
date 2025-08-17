package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.visits.domain.command.*
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitDocuments
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitPeriod
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VisitDomainService(
    private val visitRepository: VisitRepository,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
    private val visitActivitySender: VisitActivitySender
) {
    fun createVisit(command: CreateVisitCommand): Visit {
        validateCreateCommand(command)

        val visit = Visit(
            id = null,
            companyId = command.companyId,
            title = command.title.trim(),
            clientId = command.clientId,
            vehicleId = command.vehicleId,
            period = VisitPeriod(command.startDate, command.endDate),
            status = command.status,
            services = command.services.map { createVisitService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return visitRepository.save(visit)
            .also { clientStatisticsCommandService.recordVisit(visit.clientId.value.toString()) }
            .also { visitActivitySender.onVisitCreated(visit) }
    }

    fun updateVisit(visitId: VisitId, command: UpdateVisitCommand, companyId: Long): Visit {
        val existingVisit = getVisitForCompany(visitId, companyId)
        validateUpdateCommand(command, existingVisit)

        val updatedVisit = existingVisit.copy(
            title = command.title.trim(),
            period = VisitPeriod(command.startDate, command.endDate),
            services = command.services.map { updateVisitService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            updatedAt = LocalDateTime.now(),
            status = command.status,
        )

        return visitRepository.save(updatedVisit)
    }

    fun changeVisitStatus(command: ChangeVisitStatusCommand): Visit {
        val visit = getVisitForCompany(command.visitId, command.companyId)
        val updatedVisit = visit.changeStatus(command.newStatus)
        return visitRepository.save(updatedVisit)
    }

    fun deleteVisit(visitId: VisitId, companyId: Long): Boolean {
        val visit = getVisitForCompany(visitId, companyId)

        if (visit.status == VisitStatus.IN_PROGRESS) {
            throw BusinessException("Cannot delete visit that is in progress")
        }

        return visitRepository.deleteById(visitId, companyId)
    }

    fun getVisitForCompany(visitId: VisitId, companyId: Long): Visit {
        return visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
    }

    fun getVisitsForCompany(companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByCompanyId(companyId, pageable)
    }
    
    fun getVisitCountByStatus(companyId: Long, status: VisitStatus): Long {
        return visitRepository.countByStatus(companyId, status)
    }

    fun getVisitsForClient(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByClientId(clientId, companyId, pageable)
    }

    fun getVisitsForVehicle(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByVehicleId(vehicleId, companyId, pageable)
    }

    private fun validateCreateCommand(command: CreateVisitCommand) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }
        if (command.calendarColorId.isBlank()) {
            throw BusinessException("Calendar color ID cannot be blank")
        }
    }

    private fun validateUpdateCommand(command: UpdateVisitCommand, existingVisit: Visit) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }
        if (existingVisit.status == VisitStatus.COMPLETED) {
            throw BusinessException("Cannot update completed visit")
        }
    }

    private fun createVisitService(command: CreateServiceCommand): VisitService {
        val discount = if (command.discountType != null && command.discountValue != null) {
            ServiceDiscount(command.discountType, command.discountValue)
        } else null

        val finalPrice = command.finalPrice ?: discount?.applyTo(
            command.basePrice.multiply(java.math.BigDecimal.valueOf(command.quantity))
        ) ?: command.basePrice.multiply(java.math.BigDecimal.valueOf(command.quantity))

        return VisitService(
            id = java.util.UUID.randomUUID().toString(),
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    private fun updateVisitService(command: UpdateServiceCommand): VisitService {
        val discount = if (command.discountType != null && command.discountValue != null) {
            ServiceDiscount(command.discountType, command.discountValue)
        } else null

        val finalPrice = command.finalPrice ?: discount?.applyTo(
            command.basePrice.multiply(java.math.BigDecimal.valueOf(command.quantity))
        ) ?: command.basePrice.multiply(java.math.BigDecimal.valueOf(command.quantity))

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }
}