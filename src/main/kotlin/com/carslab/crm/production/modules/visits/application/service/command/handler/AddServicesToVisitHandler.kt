package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.services.application.dto.CreateServiceRequest
import com.carslab.crm.production.modules.services.application.service.ServiceCommandService
import com.carslab.crm.production.modules.visits.application.dto.AddServiceItemRequest
import com.carslab.crm.production.modules.visits.application.dto.AddServicesToVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.domain.command.AddServiceItemCommand
import com.carslab.crm.production.modules.visits.domain.command.AddServicesToVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class AddServicesToVisitHandler(
    private val aggregateService: AggregateService,
    private val serviceCommandService: ServiceCommandService
) {
    private val logger = LoggerFactory.getLogger(AddServicesToVisitHandler::class.java)

    @Transactional
    fun handle(visitId: VisitId, request: AddServicesToVisitRequest, companyId: Long): VisitResponse {
        logger.info("Adding {} services to visit: {}", request.services.size, visitId.value)

        val command = buildCommand(visitId, request, companyId)
        val updatedVisit = addServicesToVisit(command)

        logger.info("Successfully added services to visit: {}", visitId.value)
        return VisitResponse.from(updatedVisit)
    }

    private fun buildCommand(
        visitId: VisitId,
        request: AddServicesToVisitRequest,
        companyId: Long
    ): AddServicesToVisitCommand {
        val serviceCommands = request.services.map { serviceItem ->
            AddServiceItemCommand(
                serviceId = resolveServiceId(serviceItem),
                name = serviceItem.name,
                basePrice = serviceItem.basePrice,
                quantity = serviceItem.quantity,
                discountType = serviceItem.discountType?.let { EnumMappers.mapToDiscountType(it) },
                discountValue = serviceItem.discountValue,
                finalPrice = serviceItem.finalPrice,
                note = serviceItem.note,
                description = serviceItem.description,
                vatRate = serviceItem.vatRate ?: 23
            )
        }

        return AddServicesToVisitCommand(
            visitId = visitId,
            companyId = companyId,
            services = serviceCommands
        )
    }

    private fun resolveServiceId(serviceItem: AddServiceItemRequest): String {
        return serviceItem.serviceId ?: createNewService(serviceItem)
    }

    private fun createNewService(serviceItem: AddServiceItemRequest): String {
        logger.info("Creating new service: {}", serviceItem.name)

        val createServiceRequest = CreateServiceRequest(
            name = serviceItem.name,
            description = serviceItem.description,
            price = serviceItem.basePrice,
            vatRate = serviceItem.vatRate ?: 23
        )

        val createdService = serviceCommandService.createService(createServiceRequest)
        logger.info("Created new service with ID: {}", createdService.id)

        return createdService.id
    }

    private fun addServicesToVisit(command: AddServicesToVisitCommand): com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit {
        val visit = aggregateService.findById(command.visitId, command.companyId)

        validateVisitCanHaveServicesAdded(visit)

        val newServices = command.services.map { serviceCommand ->
            createVisitService(serviceCommand)
        }

        val updatedServices = visit.services + newServices
        val updatedVisit = visit.updateServices(updatedServices)

        return aggregateService.updateVisitServices(command.visitId, updatedServices, command.companyId)
    }

    private fun validateVisitCanHaveServicesAdded(visit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit) {
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.COMPLETED) {
            throw BusinessException("Cannot add services to completed visit")
        }
        if (visit.status == com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus.CANCELLED) {
            throw BusinessException("Cannot add services to cancelled visit")
        }
    }

    private fun createVisitService(serviceCommand: AddServiceItemCommand): VisitService {
        val discount = if (serviceCommand.discountType != null && serviceCommand.discountValue != null) {
            ServiceDiscount(serviceCommand.discountType, serviceCommand.discountValue)
        } else null

        val finalPrice = serviceCommand.finalPrice ?: calculateFinalPrice(
            serviceCommand.basePrice,
            serviceCommand.quantity,
            discount
        )

        return VisitService(
            id = serviceCommand.serviceId!!,
            name = serviceCommand.name,
            basePrice = serviceCommand.basePrice,
            quantity = serviceCommand.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = ServiceApprovalStatus.PENDING,
            note = serviceCommand.note
        )
    }

    private fun calculateFinalPrice(
        basePrice: BigDecimal,
        quantity: Long,
        discount: ServiceDiscount?
    ): BigDecimal {
        val totalBase = basePrice.multiply(BigDecimal.valueOf(quantity))
        return discount?.applyTo(totalBase) ?: totalBase
    }
}