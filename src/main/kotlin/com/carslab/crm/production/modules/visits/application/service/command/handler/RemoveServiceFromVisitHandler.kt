package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.visits.application.dto.RemoveServiceFromVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.domain.command.RemoveServiceFromVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RemoveServiceFromVisitHandler(
    private val aggregateService: AggregateService
) {
    private val logger = LoggerFactory.getLogger(RemoveServiceFromVisitHandler::class.java)

    @Transactional
    fun handle(visitId: VisitId, request: RemoveServiceFromVisitRequest, companyId: Long): VisitResponse {
        logger.info("Removing service {} from visit: {}", request.serviceId, visitId.value)

        val command = RemoveServiceFromVisitCommand(
            visitId = visitId,
            companyId = companyId,
            serviceId = request.serviceId,
            reason = request.reason
        )

        val updatedVisit = removeServiceFromVisit(command)

        logger.info("Successfully removed service from visit: {}", visitId.value)
        return VisitResponse.from(updatedVisit)
    }

    private fun removeServiceFromVisit(command: RemoveServiceFromVisitCommand): com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit {
        val visit = aggregateService.findById(command.visitId, command.companyId)

        validateVisitCanHaveServicesRemoved(visit)
        validateServiceExists(visit, command.serviceId)

        val updatedServices = visit.services.filter { service ->
            service.id != command.serviceId
        }

        if (updatedServices.size == visit.services.size) {
            throw EntityNotFoundException("Service not found in visit: ${command.serviceId}")
        }

        return aggregateService.updateVisitServices(command.visitId, updatedServices, command.companyId)
    }

    private fun validateVisitCanHaveServicesRemoved(visit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit) {
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.COMPLETED) {
            throw BusinessException("Cannot remove services from completed visit")
        }
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.CANCELLED) {
            throw BusinessException("Cannot remove services from cancelled visit")
        }
    }

    private fun validateServiceExists(visit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit, serviceId: String) {
        val serviceExists = visit.services.any { it.id == serviceId }
        if (!serviceExists) {
            throw EntityNotFoundException("Service not found in visit: $serviceId")
        }
    }
}