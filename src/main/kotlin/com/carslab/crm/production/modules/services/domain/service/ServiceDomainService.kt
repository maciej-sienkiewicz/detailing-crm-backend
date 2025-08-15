package com.carslab.crm.production.modules.services.domain.service

import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.dto.RelatedEntityDto
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.activities.domain.model.ActivityStatus
import com.carslab.crm.production.modules.services.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.services.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.services.domain.model.Service
import com.carslab.crm.production.modules.services.domain.model.ServiceId
import com.carslab.crm.production.modules.services.domain.repository.ServiceRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.ServiceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service as SpringService
import java.time.LocalDateTime

@SpringService
class ServiceDomainService(
    private val repository: ServiceRepository,
    private val activityCommandService: ActivityCommandService,
) {
    private val logger = LoggerFactory.getLogger(ServiceDomainService::class.java)

    fun createService(command: CreateServiceCommand): Service {
        logger.debug("Creating service '{}' for company: {}", command.name, command.companyId)

        if (repository.existsByCompanyIdAndName(command.companyId, command.name)) {
            throw BusinessException("Service with name '${command.name}' already exists")
        }

        val service = Service(
            id = ServiceId.generate(),
            companyId = command.companyId,
            name = command.name,
            description = command.description,
            price = command.price,
            vatRate = command.vatRate,
            isActive = true,
            previousVersionId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedService = repository.save(service)
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.SYSTEM,
            message = "Utworzono nową usługę: \"${savedService.name}\"",
            userId = command.userId,
            userName = command.userName,
            status = ActivityStatus.SUCCESS,
            statusText = "Usługa została pomyślnie utworzona",
            primaryEntity = null,
            relatedEntities = emptyList(),
            metadata = emptyMap())
        )
        
        logger.info("Service created: {} for company: {}", savedService.id.value, command.companyId)
        return savedService
    }

    fun updateService(command: UpdateServiceCommand): Service {
        logger.debug("Updating service: {} for company: {}", command.serviceId, command.companyId)

        val existingService = getActiveServiceForCompany(command.serviceId, command.companyId)

        if (repository.existsByCompanyIdAndName(command.companyId, command.name) &&
            existingService.name != command.name) {
            throw BusinessException("Service with name '${command.name}' already exists")
        }

        repository.deactivateById(existingService.id)

        val updatedService = existingService.update(
            name = command.name,
            description = command.description,
            price = command.price,
            vatRate = command.vatRate
        )

        val savedService = repository.save(updatedService)
        logger.info("Service updated: {} for company: {}", savedService.id.value, command.companyId)
        return savedService
    }

    fun getServicesForCompany(companyId: Long): List<Service> {
        logger.debug("Fetching services for company: {}", companyId)

        val services = repository.findActiveByCompanyId(companyId)
        logger.debug("Found {} services for company: {}", services.size, companyId)
        return services
    }

    fun getActiveServiceForCompany(serviceId: String, companyId: Long): Service {
        logger.debug("Fetching service: {} for company: {}", serviceId, companyId)

        val service = repository.findActiveById(ServiceId.of(serviceId))
            ?: throw ServiceNotFoundException("Service not found: $serviceId")

        if (!service.canBeUsedBy(companyId)) {
            logger.warn("Access denied to service: {} for company: {}", serviceId, companyId)
            throw BusinessException("Access denied to service")
        }

        return service
    }

    fun deleteService(serviceId: String, companyId: Long): Boolean {
        logger.debug("Deleting service: {} for company: {}", serviceId, companyId)

        val service = getActiveServiceForCompany(serviceId, companyId)
        repository.deactivateById(service.id)

        logger.info("Service deactivated: {} for company: {}", serviceId, companyId)
        return true
    }
}
