package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.command.*
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitCreationOrchestrator
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class VisitCommandService(
    private val visitDomainService: VisitDomainService,
    private val visitCreationOrchestrator: VisitCreationOrchestrator,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCommandService::class.java)

    fun createVisit(request: CreateVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating visit '{}' for company: {}", request.title, companyId)

        validateCreateRequest(request)

        val entities = visitCreationOrchestrator.prepareVisitEntities(request)

        val command = CreateVisitCommand(
            companyId = companyId,
            title = request.title,
            client = entities.client,
            vehicle = entities.vehicle,
            startDate = LocalDateTime.parse(request.startDate),
            endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            status = request.status?.let { EnumMappers.mapFromApiProtocolStatus(it) } ?: VisitStatus.SCHEDULED,
            services = request.selectedServices?.map { mapToCreateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = request.referralSource?.let { EnumMappers.mapFromApiReferralSource(it) },
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided ?: false,
            documentsProvided = request.documentsProvided ?: false
        )

        val visit = visitDomainService.createVisit(command)
        logger.info("Visit created successfully: {}", visit.id?.value)

        return VisitResponse.from(visit)
    }

    fun updateVisit(visitId: String, request: UpdateCarReceptionCommand): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating visit: {} for company: {}", visitId, companyId)

        validateUpdateRequest(request)

        val command = UpdateVisitCommand(
            title = request.title,
            startDate = LocalDateTime.parse(request.startDate),
            endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
            services = request.selectedServices?.map { mapToUpdateServiceCommand(it) } ?: emptyList(),
            notes = request.notes,
            referralSource = EnumMappers.mapToReferralSource(request.referralSource?.toString()),
            appointmentId = request.appointmentId,
            calendarColorId = request.calendarColorId,
            keysProvided = request.keysProvided ?: false,
            documentsProvided = request.documentsProvided ?: false,
            status = EnumMappers.mapToVisitStatus(request.status.toString())
        )

        val visit = visitDomainService.updateVisit(VisitId.of(visitId), command, companyId)
        logger.info("Visit updated successfully: {}", visitId)

        return VisitResponse.from(visit)
    }

    fun changeVisitStatus(visitId: String, request: ChangeStatusRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Changing status for visit: {} to {} for company: {}", visitId, request.status, companyId)

        val command = ChangeVisitStatusCommand(
            visitId = VisitId.of(visitId),
            newStatus = request.status,
            reason = request.reason,
            companyId = companyId
        )

        val visit = visitDomainService.changeVisitStatus(command)
        logger.info("Visit status changed successfully: {}", visitId)

        return VisitResponse.from(visit)
    }

    fun deleteVisit(visitId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting visit: {} for company: {}", visitId, companyId)

        val deleted = visitDomainService.deleteVisit(VisitId.of(visitId), companyId)
        if (deleted) {
            logger.info("Visit deleted successfully: {}", visitId)
        } else {
            logger.warn("Visit not found for deletion: {}", visitId)
        }
    }

    fun release(visitId: String, request: ReleaseVehicleRequest): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Releasing vehicle for visit: {} for company: {}", visitId, companyId)

        visitDomainService.releaseVehicle(visitId, request)
        logger.info("Vehicle released successfully for visit: {}", visitId)

        return true
    }

    private fun validateCreateRequest(request: CreateVisitRequest) {
        if (request.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
    }

    private fun validateUpdateRequest(request: UpdateCarReceptionCommand) {
        if (request.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
    }

    private fun mapToCreateServiceCommand(request: com.carslab.crm.modules.visits.api.commands.CreateServiceCommand): CreateServiceCommand {
        return CreateServiceCommand(
            name = request.name,
            basePrice = CalculationUtils.anyToBigDecimal(request.price),
            quantity = request.quantity,
            discountType = EnumMappers.mapToDiscountType(request.discountType?.toString()),
            discountValue = request.discountValue?.let { CalculationUtils.anyToBigDecimal(it) },
            finalPrice = request.finalPrice?.let { CalculationUtils.anyToBigDecimal(it) },
            approvalStatus = EnumMappers.mapToServiceApprovalStatus(request.approvalStatus?.toString()),
            note = request.note
        )
    }

    private fun mapToUpdateServiceCommand(request: com.carslab.crm.modules.visits.api.commands.UpdateServiceCommand): UpdateServiceCommand {
        return UpdateServiceCommand(
            id = request.id,
            name = request.name,
            basePrice = CalculationUtils.anyToBigDecimal(request.price),
            quantity = request.quantity,
            discountType = EnumMappers.mapToDiscountType(request.discountType?.toString()),
            discountValue = request.discountValue?.let { CalculationUtils.anyToBigDecimal(it) },
            finalPrice = request.finalPrice?.let { CalculationUtils.anyToBigDecimal(it) },
            approvalStatus = EnumMappers.mapToServiceApprovalStatus(request.approvalStatus?.toString()),
            note = request.note
        )
    }
}