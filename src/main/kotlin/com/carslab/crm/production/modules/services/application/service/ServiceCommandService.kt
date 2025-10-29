package com.carslab.crm.production.modules.services.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.services.application.dto.CreateServiceRequest
import com.carslab.crm.production.modules.services.application.dto.ServiceResponse
import com.carslab.crm.production.modules.services.application.dto.UpdateServiceRequest
import com.carslab.crm.production.modules.services.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.services.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.services.domain.service.ServiceDomainService
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.exception.UserNotFoundException
import com.carslab.crm.production.shared.presentation.mapper.PriceMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ServiceCommandService(
    private val domainService: ServiceDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ServiceCommandService::class.java)

    fun createService(request: CreateServiceRequest): ServiceResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating service '{}' for company: {}", request.name, companyId)

        val priceValueObject = PriceValueObject.createFromInput(
            inputValue = request.price.inputPrice,
            inputType = PriceMapper.toDomain(request.price.inputType),
            vatRate = request.vatRate
        )

        val command = CreateServiceCommand(
            companyId = companyId,
            name = request.name.trim(),
            description = request.description?.trim(),
            price = priceValueObject,
            vatRate = request.vatRate,
            userId = securityContext.getCurrentUserId()
                ?: throw UserNotFoundException("User ID not found in security context"),
            userName = securityContext.getCurrentUserName()
                ?: throw UserNotFoundException("User name not found in security context")
        )

        val service = domainService.createService(command)
        logger.info("Service created successfully: {}", service.id.value)

        return ServiceResponse.from(service)
    }

    fun updateService(serviceId: String, request: UpdateServiceRequest): ServiceResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating service: {} for company: {}", serviceId, companyId)

        val priceValueObject = PriceValueObject.createFromInput(
            inputValue = request.price.inputPrice,
            inputType = PriceMapper.toDomain(request.price.inputType),
            vatRate = request.vatRate
        )

        val command = UpdateServiceCommand(
            serviceId = serviceId,
            companyId = companyId,
            name = request.name.trim(),
            description = request.description?.trim(),
            price = priceValueObject,
            vatRate = request.vatRate
        )

        val service = domainService.updateService(command)
        logger.info("Service updated successfully: {}", service.id.value)

        return ServiceResponse.from(service)
    }

    fun deleteService(serviceId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting service: {} for company: {}", serviceId, companyId)

        val deleted = domainService.deleteService(serviceId, companyId)
        if (deleted) {
            logger.info("Service deleted successfully: {}", serviceId)
        } else {
            logger.warn("Service not found for deletion: {}", serviceId)
        }
    }
}