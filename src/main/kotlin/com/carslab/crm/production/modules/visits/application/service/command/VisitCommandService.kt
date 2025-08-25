package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientWithStatisticsResponse
import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.command.*
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.ClientDetails
import com.carslab.crm.production.modules.visits.domain.service.VehicleDetails
import com.carslab.crm.production.modules.visits.domain.service.VisitCreationOrchestrator
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class VisitCommandService(
    private val visitDomainService: VisitDomainService,
    private val clientCommandService: ClientCommandService,
    private val visitCreationOrchestrator: VisitCreationOrchestrator,
    private val securityContext: SecurityContext,
    private val clientQueryService: ClientQueryService
) {
    private val logger = LoggerFactory.getLogger(VisitCommandService::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createVisit(request: CreateVisitRequest): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating visit '{}' for company: {}", request.title, companyId)

        validateCreateRequest(request)

        val clientDetails = ClientDetails(
            ownerId = request.ownerId,
            email = request.email,
            phone = request.phone,
            name = request.ownerName,
            companyName = request.companyName,
            taxId = request.taxId,
            address = request.address
        )
        val vehicleDetails = VehicleDetails(
            make = request.make,
            model = request.model,
            licensePlate = request.licensePlate!!,
            productionYear = request.productionYear,
            vin = request.vin,
            color = request.color,
            mileage = request.mileage,
            ownerId = request.ownerId
        )
        val entities = visitCreationOrchestrator.prepareVisitEntities(clientDetails, vehicleDetails)
        updateClientInfo(entities.client.id, UpdateClientRequest(
            firstName = request.ownerName.split(" ").first(),
            lastName = request.ownerName.split(" ").last(),
            email = request.email ?: "",
            phone = request.phone ?: "",
            address = request.address,
            company = request.companyName,
            taxId = request.taxId
        ))

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
            documentsProvided = request.documentsProvided ?: false,
            deliveryPerson = request.deliveryPerson?.let {
                DeliveryPerson(
                    id = it.id,
                    name = it.name,
                    phone = it.phone
                )
            }
        )

        val visit = visitDomainService.createVisit(command)
        logger.info("Visit created successfully: {}", visit.id?.value)

        return VisitResponse.from(visit)
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateClientInfo(clientId: String, request: UpdateClientRequest): ClientResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating client info for client: {} in company: {}", clientId, companyId)
        
        val existingClient = clientQueryService.getClient(clientId).client

        if (!hasAtLeastOneChange(request, existingClient)) {
            logger.info("No changes detected for client: {}. Skipping update.", clientId)
            return existingClient
        }

        val updatedClient = clientCommandService.updateClient(
            clientId = clientId,
            request = request
        )

        logger.info("Client info updated successfully for client: {}", clientId)
        return updatedClient
    }
    
    private fun hasAtLeastOneChange(request: UpdateClientRequest, client: ClientResponse): Boolean {
        return (request.email != client.email) ||
               (request.phone != client.phone) ||
               (request.company != client.company) ||
               (request.taxId != client.taxId) ||
               (request.address != client.address)
    }

    fun updateVisit(visitId: String, request: UpdateCarReceptionCommand): VisitResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating visit: {} for company: {}", visitId, companyId)

        validateUpdateRequest(request)
        
        val actualVisit = visitDomainService.getVisitForCompany(VisitId.of(visitId), companyId)
        val association = if(actualVisit.status == VisitStatus.SCHEDULED && request.status == ApiProtocolStatus.IN_PROGRESS && request.deliveryPerson != null) {
           visitCreationOrchestrator.prepareVisitEntities(
               ClientDetails(
                   ownerId = request.deliveryPerson!!.id?.toLong(),
                   name = request.deliveryPerson.name,
                   phone = request.deliveryPerson.phone,
               ),
               VehicleDetails(
                   make = request.make,
                   model = request.model,
                   licensePlate = request.licensePlate,
               )
           )
        } else null
        
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
            status = EnumMappers.mapToVisitStatus(request.status.toString()),
            deliveryPerson = request.deliveryPerson?.copy(id = association?.client?.id)
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