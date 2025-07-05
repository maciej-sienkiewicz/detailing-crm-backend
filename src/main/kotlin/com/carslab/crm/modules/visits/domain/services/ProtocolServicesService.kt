package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand
import com.carslab.crm.modules.visits.application.mappers.ProtocolServiceMapper
import com.carslab.crm.modules.visits.domain.exceptions.ProtocolServicesUpdateException
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import com.carslab.crm.modules.visits.domain.valueobjects.ServicesUpdateResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProtocolServicesService(
    private val protocolServicesRepository: ProtocolServicesRepository,
    private val serviceMapper: ProtocolServiceMapper
) {
    private val logger = LoggerFactory.getLogger(ProtocolServicesService::class.java)

    @Transactional
    fun updateProtocolServices(
        protocolId: ProtocolId,
        serviceCommands: List<UpdateServiceCommand>
    ): ServicesUpdateResult {
        return try {
            val serviceModels = serviceCommands.map(serviceMapper::toCreateServiceModel)
            val savedServiceIds = protocolServicesRepository.saveServices(serviceModels, protocolId)

            logger.info("Updated ${savedServiceIds.size} services for protocol: ${protocolId.value}")

            ServicesUpdateResult.success(
                updatedCount = savedServiceIds.size,
                serviceIds = savedServiceIds,
                totalAmount = serviceCommands.sumOf { it.finalPrice ?: it.basePrice }
            )
        } catch (e: Exception) {
            logger.error("Failed to update services for protocol: ${protocolId.value}", e)
            throw ProtocolServicesUpdateException("Failed to update protocol services", e)
        }
    }
}