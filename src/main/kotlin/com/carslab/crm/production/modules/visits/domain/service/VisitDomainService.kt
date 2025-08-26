package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.ChangeVisitStatusCommand
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.factory.VisitFactory
import com.carslab.crm.production.modules.visits.domain.service.policy.VisitBusinessPolicy
import com.carslab.crm.production.modules.visits.domain.service.validator.VisitCommandValidator
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class VisitDomainService(
    private val visitRepository: VisitRepository,
    private val visitFactory: VisitFactory,
    private val commandValidator: VisitCommandValidator,
    private val businessPolicy: VisitBusinessPolicy
) {
    fun createVisit(command: CreateVisitCommand): Visit {
        commandValidator.validateCreateCommand(command)

        val visit = visitFactory.createVisit(command)

        return visitRepository.save(visit)
    }

    fun updateVisit(visitId: VisitId, command: UpdateVisitCommand, companyId: Long): Visit {
        val existingVisit = getVisitForCompany(visitId, companyId)
        commandValidator.validateUpdateCommand(command, existingVisit)

        val updatedVisit = visitFactory.updateVisit(existingVisit, command)

        return visitRepository.save(updatedVisit)
    }

    fun changeVisitStatus(command: ChangeVisitStatusCommand): Visit {
        val visit = getVisitForCompany(command.visitId, command.companyId)

        businessPolicy.validateStatusTransition(visit.status, command.newStatus)

        val updatedVisit = visit.changeStatus(command.newStatus)

        return visitRepository.save(updatedVisit)
    }

    fun deleteVisit(visitId: VisitId, companyId: Long): Boolean {
        val visit = getVisitForCompany(visitId, companyId)

        businessPolicy.validateDeletion(visit)

        return visitRepository.deleteById(visitId, companyId)
    }

    fun getVisitForCompany(visitId: VisitId, companyId: Long): Visit {
        return visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
    }

    fun getVisitCountByStatus(companyId: Long, status: VisitStatus): Long {
        return visitRepository.countByStatus(companyId, status)
    }
    
    fun getVisitsForVehicle(vehicleId: VehicleId, companyId: Long, pageable: Pageable) =
        visitRepository.findByVehicleId(vehicleId, companyId, pageable)
}