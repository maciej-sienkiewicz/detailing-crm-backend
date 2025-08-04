package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
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

    fun getCurrentServices(protocolId: ProtocolId): List<ProtocolServiceView> {
        return try {
            protocolServicesRepository.findByProtocolId(protocolId)
        } catch (e: Exception) {
            logger.warn("Failed to get services for protocol: ${protocolId.value}", e)
            emptyList()
        }
    }

    @Transactional
    fun updateProtocolServices(
        protocolId: ProtocolId,
        serviceCommands: List<UpdateServiceCommand>
    ): ServicesUpdateResult {
        return try {
            val currentServices = getCurrentServices(protocolId)

            if (!hasActualChanges(currentServices, serviceCommands)) {
                logger.debug("No actual changes detected for protocol services: ${protocolId.value}")
                return ServicesUpdateResult.noChanges()
            }

            val serviceModels = serviceCommands.map(serviceMapper::toCreateServiceModel)
            val savedServiceIds = protocolServicesRepository.saveServices(serviceModels, protocolId)

            logger.info("Updated ${savedServiceIds.size} services for protocol: ${protocolId.value}")

            ServicesUpdateResult.success(
                updatedCount = savedServiceIds.size,
                serviceIds = savedServiceIds,
                totalAmount = serviceCommands.sumOf { it.finalPrice ?: it.basePrice },
                serviceNames = serviceCommands.map { it.name }
            )
        } catch (e: Exception) {
            logger.error("Failed to update services for protocol: ${protocolId.value}", e)
            throw ProtocolServicesUpdateException("Failed to update protocol services", e)
        }
    }

    private fun hasActualChanges(
        currentServices: List<ProtocolServiceView>,
        newServices: List<UpdateServiceCommand>
    ): Boolean {
        if (currentServices.size != newServices.size) {
            return true
        }

        val currentByName = currentServices.associateBy { it.name }

        return newServices.any { newService ->
            val current = currentByName[newService.name] ?: return true

            current.basePrice.amount != newService.basePrice ||
                    current.finalPrice.amount != (newService.finalPrice ?: newService.basePrice) ||
                    current.quantity != newService.quantity ||
                    current.approvalStatus.name != newService.approvalStatus ||
                    current.note != newService.note
        }
    }
}