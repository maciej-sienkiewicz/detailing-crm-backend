package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.visits.application.dto.UpdateVisitServicesRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.domain.command.ServiceUpdateItem
import com.carslab.crm.production.modules.visits.domain.command.UpdateVisitServicesCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.shared.domain.value_objects.DiscountType
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.production.shared.presentation.mapper.PriceMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateVisitServicesHandler(
    private val aggregateService: AggregateService
) {
    private val logger = LoggerFactory.getLogger(UpdateVisitServicesHandler::class.java)

    @Transactional
    fun handle(visitId: VisitId, request: UpdateVisitServicesRequest, companyId: Long): VisitResponse {
        logger.info("Updating services for visit: {}", visitId.value)

        val command = buildCommand(visitId, request, companyId)
        val updatedVisit = updateVisitServices(command)

        logger.info("Successfully updated services for visit: {}", visitId.value)
        return VisitResponse.from(updatedVisit)
    }

    private fun buildCommand(
        visitId: VisitId,
        request: UpdateVisitServicesRequest,
        companyId: Long
    ): UpdateVisitServicesCommand {
        val serviceUpdates = request.services.map { serviceItem ->
            val priceValueObject = PriceValueObject.createFromInput(
                inputValue = serviceItem.price.inputPrice,
                inputType = PriceMapper.toDomain(serviceItem.price.inputType),
                vatRate = serviceItem.vatRate
            )

            ServiceUpdateItem(
                name = serviceItem.name.trim(),
                basePrice = priceValueObject,
                quantity = serviceItem.quantity,
                discountType = serviceItem.discountType?.let { DiscountType.valueOf(it.uppercase()) },
                discountValue = serviceItem.discountValue,
                approvalStatus = EnumMappers.mapToServiceApprovalStatus(serviceItem.approvalStatus),
                note = serviceItem.note?.trim()
            )
        }

        return UpdateVisitServicesCommand(
            visitId = visitId,
            companyId = companyId,
            serviceUpdates = serviceUpdates
        )
    }

    private fun updateVisitServices(command: UpdateVisitServicesCommand): com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit {
        val visit = aggregateService.findById(command.visitId, command.companyId)

        validateVisitCanBeUpdated(visit)

        val updatedServices = updateServicesInVisit(visit.services, command.serviceUpdates)

        return aggregateService.updateVisitServices(command.visitId, updatedServices, command.companyId)
    }

    private fun validateVisitCanBeUpdated(visit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit) {
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.COMPLETED) {
            throw BusinessException("Cannot update services for completed visit")
        }
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.CANCELLED) {
            throw BusinessException("Cannot update services for cancelled visit")
        }
    }

    private fun updateServicesInVisit(
        existingServices: List<VisitService>,
        serviceUpdates: List<ServiceUpdateItem>
    ): List<VisitService> {
        val updatedServices = existingServices.toMutableList()

        serviceUpdates.forEach { update ->
            val serviceIndex = updatedServices.indexOfFirst {
                it.name.trim().equals(update.name.trim(), ignoreCase = true)
            }

            if (serviceIndex != -1) {
                val existingService = updatedServices[serviceIndex]
                val updatedService = updateExistingService(existingService, update)
                updatedServices[serviceIndex] = updatedService

                logger.debug("Updated service '{}' in visit", update.name)
            } else {
                logger.warn("Service '{}' not found in visit for update", update.name)
                throw EntityNotFoundException("Service '${update.name}' not found in visit")
            }
        }

        return updatedServices
    }

    private fun updateExistingService(
        existingService: VisitService,
        update: ServiceUpdateItem
    ): VisitService {
        val discount = if (update.discountType != null && update.discountValue != null) {
            ServiceDiscount(update.discountType, update.discountValue)
        } else null

        return existingService.copy(
            basePrice = update.basePrice,
            quantity = update.quantity,
            discount = discount,
            approvalStatus = update.approvalStatus,
            note = update.note
        )
    }
}