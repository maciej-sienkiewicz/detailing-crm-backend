package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.ports.ProtocolServicesRepository
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.modules.visits.domain.events.*
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolService
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.activities.domain.services.ActivityService
import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.visits.api.mappers.ServiceMappers
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UpdateProtocolServicesCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolServicesRepository: ProtocolServicesRepository,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext,
    private val activityService: ActivityService
) : CommandHandler<UpdateProtocolServicesCommand, Unit> {

    private val logger = LoggerFactory.getLogger(UpdateProtocolServicesCommandHandler::class.java)

    @Transactional
    override fun handle(command: UpdateProtocolServicesCommand) {
        logger.info("Updating services for protocol: ${command.protocolId}")

        val protocolId = ProtocolId(command.protocolId)
        val existingProtocol = protocolRepository.findById(protocolId)
            ?: throw ResourceNotFoundException("Protocol", command.protocolId)

        val currentServices = protocolServicesRepository.findByProtocolId(protocolId)
        val newServices = command.services

        val changes = detectServiceChanges(currentServices, newServices)

        if (changes.isEmpty()) {
            logger.debug("No service changes detected for protocol: ${command.protocolId}")
            return
        }

        val serviceModels = newServices.map { serviceCommand ->
            ServiceMappers.toCreateServiceModel(serviceCommand)
        }

        protocolServicesRepository.saveServices(serviceModels, protocolId)

        protocolRepository.updateAuditLog(protocolId)
        
        createServiceChangeActivity(command.protocolId, existingProtocol.title, changes)

        logger.info("Successfully updated services for protocol: ${command.protocolId}")
    }

    private fun detectServiceChanges(
        currentServices: List<com.carslab.crm.domain.model.view.protocol.ProtocolServiceView>,
        newServices: List<com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand>
    ): List<ServiceChange> {
        val changes = mutableListOf<ServiceChange>()

        val currentByName = currentServices.associateBy { it.name }
        val newByName = newServices.associateBy { it.name }

        newServices.forEach { newService ->
            val existing = currentByName[newService.name]
            if (existing == null) {
                changes.add(ServiceChange.Added(newService.name, newService.finalPrice ?: newService.basePrice))
            } else {
                val existingFinalPrice = existing.finalPrice.amount
                val newFinalPrice = newService.finalPrice ?: newService.basePrice

                if (existingFinalPrice != newFinalPrice) {
                    changes.add(ServiceChange.PriceChanged(
                        serviceName = newService.name,
                        oldPrice = existingFinalPrice,
                        newPrice = newFinalPrice
                    ))
                }

                if (existing.quantity != newService.quantity) {
                    changes.add(ServiceChange.QuantityChanged(
                        serviceName = newService.name,
                        oldQuantity = existing.quantity,
                        newQuantity = newService.quantity
                    ))
                }
            }
        }

        currentServices.forEach { currentService ->
            if (!newByName.containsKey(currentService.name)) {
                changes.add(ServiceChange.Removed(currentService.name, currentService.finalPrice.amount))
            }
        }

        return changes
    }

    private fun createServiceChangeActivity(protocolId: String, protocolTitle: String, changes: List<ServiceChange>) {
        if (changes.isEmpty()) return

        try {
            val changeMessages = changes.map { change ->
                when (change) {
                    is ServiceChange.Added -> "Dodano usługę: ${change.serviceName} (${String.format("%.2f", change.price)} PLN)"
                    is ServiceChange.Removed -> "Usunięto usługę: ${change.serviceName} (${String.format("%.2f", change.price)} PLN)"
                    is ServiceChange.PriceChanged -> "Zmieniono cenę usługi ${change.serviceName}: ${String.format("%.2f", change.oldPrice)} PLN → ${String.format("%.2f", change.newPrice)} PLN"
                    is ServiceChange.QuantityChanged -> "Zmieniono ilość usługi ${change.serviceName}: ${change.oldQuantity} → ${change.newQuantity}"
                }
            }

            val message = if (changes.size == 1) {
                changeMessages.first()
            } else {
                "Zaktualizowano ${changes.size} usług w protokole"
            }

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = message,
                entityType = EntityType.PROTOCOL,
                entityId = protocolId,
                entities = listOf(
                    ActivityEntityReadModel(
                        id = protocolId,
                        type = EntityType.PROTOCOL,
                        displayName = protocolTitle
                    )
                ),
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = changeMessages.joinToString("\n"),
                    servicesList = changes.mapNotNull {
                        when (it) {
                            is ServiceChange.Added -> it.serviceName
                            is ServiceChange.PriceChanged -> it.serviceName
                            is ServiceChange.QuantityChanged -> it.serviceName
                            else -> null
                        }
                    }
                ),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        } catch (e: Exception) {
            logger.warn("Failed to create activity for service changes in protocol: $protocolId", e)
        }
    }

    private sealed class ServiceChange {
        data class Added(val serviceName: String, val price: Double) : ServiceChange()
        data class Removed(val serviceName: String, val price: Double) : ServiceChange()
        data class PriceChanged(val serviceName: String, val oldPrice: Double, val newPrice: Double) : ServiceChange()
        data class QuantityChanged(val serviceName: String, val oldQuantity: Long, val newQuantity: Long) : ServiceChange()
    }
}