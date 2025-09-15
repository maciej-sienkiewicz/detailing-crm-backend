package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.AddServicesToVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.ChangeStatusRequest
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.RemoveServiceFromVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.UpdateVisitServicesRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.handler.AddServicesToVisitHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.RemoveServiceFromVisitHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.UpdateVisitServicesHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.VisitCreateHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.VisitUpdateHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.VisitStatusChangeHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.VisitDeleteHandler
import com.carslab.crm.production.modules.visits.application.service.command.handler.VisitReleaseHandler
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VisitCommandService(
    private val createHandler: VisitCreateHandler,
    private val updateHandler: VisitUpdateHandler,
    private val statusChangeHandler: VisitStatusChangeHandler,
    private val deleteHandler: VisitDeleteHandler,
    private val releaseHandler: VisitReleaseHandler,
    private val addServicesHandler: AddServicesToVisitHandler,
    private val removeServiceHandler: RemoveServiceFromVisitHandler,
    private val updateVisitServicesHandler: UpdateVisitServicesHandler,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCommandService::class.java)

    fun createVisit(request: CreateVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating visit '{}' for company: {}", request.title, companyId)

        val result = createHandler.handle(request, companyId)

        logger.info("Visit created successfully: {}", result.id)
        return result
    }

    fun updateVisit(visitId: String, request: UpdateCarReceptionCommand): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating visit: {} for company: {}", visitId, companyId)

        val result = updateHandler.handle(VisitId.of(visitId), request, companyId)

        logger.info("Visit updated successfully: {}", visitId)
        return result
    }

    fun changeVisitStatus(visitId: String, request: ChangeStatusRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Changing status for visit: {} to {} for company: {}", visitId, request.status, companyId)

        val result = statusChangeHandler.handle(VisitId.of(visitId), request, companyId)

        logger.info("Visit status changed successfully: {}", visitId)
        return result
    }

    fun deleteVisit(visitId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting visit: {} for company: {}", visitId, companyId)

        val deleted = deleteHandler.handle(VisitId.of(visitId), companyId)

        if (deleted) {
            logger.info("Visit deleted successfully: {}", visitId)
        } else {
            logger.warn("Visit not found for deletion: {}", visitId)
        }
    }

    fun release(visitId: String, request: ReleaseVehicleRequest): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Releasing vehicle for visit: {} for company: {}", visitId, companyId)

        val result = releaseHandler.handle(visitId, request, companyId)

        logger.info("Vehicle released successfully for visit: {}", visitId)
        return result
    }

    fun addServicesToVisit(visitId: String, request: AddServicesToVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Adding services to visit: {} for company: {}", visitId, companyId)

        val result = addServicesHandler.handle(VisitId.of(visitId), request, companyId)

        logger.info("Services added successfully to visit: {}", visitId)
        return result
    }

    fun removeServiceFromVisit(visitId: String, request: RemoveServiceFromVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Removing service from visit: {} for company: {}", visitId, companyId)

        val result = removeServiceHandler.handle(VisitId.of(visitId), request, companyId)

        logger.info("Service removed successfully from visit: {}", visitId)
        return result
    }

    fun updateVisitServices(visitId: String, request: UpdateVisitServicesRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating services for visit: {} for company: {}", visitId, companyId)

        val result = updateVisitServicesHandler.handle(VisitId.of(visitId), request, companyId)

        logger.info("Services updated successfully for visit: {}", visitId)
        return result
    }
}