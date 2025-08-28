package com.carslab.crm.production.modules.visits.domain.service.aggregate

import com.carslab.crm.production.modules.visits.domain.command.ChangeVisitStatusCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.factory.VisitFactory
import com.carslab.crm.production.modules.visits.domain.policy.VisitBusinessPolicy
import com.carslab.crm.production.modules.visits.domain.validator.VisitCommandValidator
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class VisitModificationService(
    private val visitRepository: VisitRepository,
    private val visitFactory: VisitFactory,
    private val commandValidator: VisitCommandValidator,
    private val businessPolicy: VisitBusinessPolicy,
) {

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
    
    private fun getVisitForCompany(visitId: VisitId, companyId: Long) = 
        visitRepository.findById(visitId, companyId) ?:
            throw EntityNotFoundException("Visit not found: ${visitId.value}")
}