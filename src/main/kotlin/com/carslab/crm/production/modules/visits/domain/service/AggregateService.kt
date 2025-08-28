package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.ChangeVisitStatusCommand
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.aggregate.VisitCompletionService
import com.carslab.crm.production.modules.visits.domain.service.aggregate.VisitCreationService
import com.carslab.crm.production.modules.visits.domain.service.aggregate.VisitModificationService
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AggregateService(
    private val creationService: VisitCreationService,
    private val modificationService: VisitModificationService,
    private val completionService: VisitCompletionService,
    private val visitRepository: VisitRepository,
) {

    fun createVisit(command: CreateVisitCommand): Visit {
        return creationService.createVisit(command)
    }

    fun updateVisit(visitId: VisitId, command: UpdateVisitCommand, companyId: Long): Visit {
        return modificationService.updateVisit(visitId, command, companyId)
    }

    fun changeVisitStatus(command: ChangeVisitStatusCommand): Visit {
        return modificationService.changeVisitStatus(command)
    }

    fun deleteVisit(visitId: VisitId, companyId: Long): Boolean {
        return modificationService.deleteVisit(visitId, companyId)
    }

    fun releaseVehicle(visitId: VisitId, request: ReleaseVehicleRequest, companyId: Long): Boolean {
        return completionService.releaseVehicle(visitId, request, companyId)
    }
    
    fun findById(visitId: VisitId, companyId: Long): Visit {
        return visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
    }
    
    fun getVisitCountByStatus(companyId: Long, status: VisitStatus): Long {
        return visitRepository.countByStatus(companyId, status)
    }
    
    fun getByVehicleId(vehicleId: VehicleId, companyId: Long, page: Pageable): Page<Visit> {
        return visitRepository.findByVehicleId(vehicleId, companyId, page)
    }
}