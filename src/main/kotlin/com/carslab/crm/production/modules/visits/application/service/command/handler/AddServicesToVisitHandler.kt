package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.services.application.dto.CreateServiceRequest
import com.carslab.crm.production.modules.services.application.service.ServiceCommandService
import com.carslab.crm.production.modules.visits.application.dto.AddServiceItemRequest
import com.carslab.crm.production.modules.visits.application.dto.AddServicesToVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.domain.command.AddServiceItemCommand
import com.carslab.crm.production.modules.visits.domain.command.AddServicesToVisitCommand
import com.carslab.crm.production.modules.visits.domain.command.ChangeVisitStatusCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import com.carslab.crm.production.modules.visits.infrastructure.mapper.EnumMappers
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.presentation.dto.PriceDto
import com.carslab.crm.production.shared.presentation.mapper.PriceMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
            .also { visit ->
                if (visit.status == VisitStatus.READY_FOR_PICKUP) {
                    aggregateService.changeVisitStatus(ChangeVisitStatusCommand(
                        visitId = visitId,
                        newStatus = VisitStatus.IN_PROGRESS,
                        reason = "Dodano nową usługę.",
                        companyId = companyId
                    ))
                }
            }

        logger.info("Successfully added services to visit: {}", visitId.value)
        return VisitResponse.from(updatedVisit)
    }

    private fun buildCommand(
        visitId: VisitId,
        request: AddServicesToVisitRequest,
        companyId: Long
    ): AddServicesToVisitCommand {
        val serviceCommands = request.services.map { serviceItem ->
            val priceValueObject = PriceValueObject.createFromInput(
                inputValue = serviceItem.price.inputPrice,
                inputType = PriceMapper.toDomain(serviceItem.price.inputType),
                vatRate = serviceItem.vatRate
            )

            AddServiceItemCommand(
                serviceId = resolveServiceId(serviceItem),
                name = serviceItem.name,
                basePrice = priceValueObject,
                quantity = serviceItem.quantity,
                discountType = serviceItem.discountType?.let { EnumMappers.mapToDiscountType(it) },
                discountValue = serviceItem.discountValue,
                note = serviceItem.note,
                description = serviceItem.description,
                vatRate = serviceItem.vatRate
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
            price = serviceItem.price,
            vatRate = serviceItem.vatRate
        )

        val createdService = serviceCommandService.createService(createServiceRequest)
        logger.info("Created new service with ID: {}", createdService.id)

        return createdService.id
    }

    private fun addServicesToVisit(command: AddServicesToVisitCommand): Visit {
        val visit = aggregateService.findById(command.visitId, command.companyId)

        validateVisitCanHaveServicesAdded(visit)

        val newServices = command.services.map { serviceCommand ->
            createVisitService(serviceCommand)
        }

        val updatedServices = visit.services + newServices

        return aggregateService.updateVisitServices(command.visitId, updatedServices, command.companyId)
    }

    private fun validateVisitCanHaveServicesAdded(visit: Visit) {
        if (visit.status == VisitStatus.COMPLETED) {
            throw BusinessException("Cannot add services to completed visit")
        }
        if (visit.status == VisitStatus.CANCELLED) {
            throw BusinessException("Cannot add services to cancelled visit")
        }
    }

    private fun createVisitService(serviceCommand: AddServiceItemCommand): VisitService {
        val discount = if (serviceCommand.discountType != null && serviceCommand.discountValue != null) {
            ServiceDiscount(serviceCommand.discountType, serviceCommand.discountValue)
        } else null

        return VisitService(
            id = serviceCommand.serviceId!!,
            name = serviceCommand.name,
            basePrice = serviceCommand.basePrice,
            quantity = serviceCommand.quantity,
            discount = discount,
            approvalStatus = ServiceApprovalStatus.PENDING,
            note = serviceCommand.note
        )
    }
}