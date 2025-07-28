package com.carslab.crm.modules.visits.infrastructure.events

import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.modules.visits.domain.events.ProtocolReadyForPickupEvent
import com.carslab.crm.modules.visits.domain.events.ProtocolServicesUpdatedEvent
import com.carslab.crm.modules.visits.domain.events.ProtocolStatusChangedEvent
import com.carslab.crm.modules.visits.domain.events.ProtocolUpdatedEvent
import com.carslab.crm.modules.visits.domain.events.ProtocolWorkStartedEvent
import com.carslab.crm.modules.visits.domain.valueobjects.ProtocolUpdateResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProtocolEventPublisher(
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(ProtocolEventPublisher::class.java)

    fun publishUpdateEvents(result: ProtocolUpdateResult, user: CurrentUser) {
        try {
            publishStatusChangeEvents(result, user)
            publishServicesUpdateEvents(result, user)
            publishGeneralUpdateEvent(result, user)
        } catch (e: Exception) {
            logger.warn("Failed to publish events for protocol: ${result.protocolId.value}", e)
            // Events failures nie powinny przerywać głównego procesu
        }
    }

    private fun publishStatusChangeEvents(result: ProtocolUpdateResult, user: CurrentUser) {
        if (result.statusChangeResult.hasChanged()) {
            // Specjalne eventy dla konkretnych statusów
            when (result.newStatus) {
                ProtocolStatus.IN_PROGRESS -> publishWorkStartedEvent(result, user)
                ProtocolStatus.READY_FOR_PICKUP -> publishReadyForPickupEvent(result, user)
                else -> publishStatusChanged(result, user)
            }
        }
    }

    private fun publishServicesUpdateEvents(result: ProtocolUpdateResult, user: CurrentUser) {
        if (result.servicesUpdateResult.hasChanges()) {
            eventPublisher.publish(
                ProtocolServicesUpdatedEvent(
                    protocolId = result.protocolId.value,
                    servicesCount = result.servicesUpdateResult.updatedCount,
                    totalAmount = result.servicesUpdateResult.totalAmount,
                    changedServices = result.servicesUpdateResult.serviceNames,
                    companyId = user.companyId,
                    userId = user.id,
                    userName = user.name
                )
            )
        }
    }

    private fun publishWorkStartedEvent(result: ProtocolUpdateResult, user: CurrentUser) {
        eventPublisher.publish(
            ProtocolWorkStartedEvent(
                protocolId = result.protocolId.value,
                protocolTitle = result.updatedProtocol.title,
                assignedTechnicians = listOf(user.name),
                estimatedCompletionTime = result.updatedProtocol.period.endDate.toString(),
                plannedServices = result.servicesUpdateResult.serviceNames,
                companyId = user.companyId,
                userId = user.id,
                userName = user.name,
                clientId = result.updatedProtocol.client.id.toString(),
                clientName = result.updatedProtocol.client.name,
                vehicleId = result.updatedProtocol.vehicle.id?.value.toString(),
                vehicleDisplayName = "${result.updatedProtocol.vehicle.make} ${result.updatedProtocol.vehicle.model} (${result.updatedProtocol.vehicle.licensePlate})".trim()
            )
        )
    }

    private fun publishReadyForPickupEvent(result: ProtocolUpdateResult, user: CurrentUser) {
        eventPublisher.publish(
            ProtocolReadyForPickupEvent(
                protocolId = result.protocolId.value,
                protocolTitle = result.updatedProtocol.title,
                clientId = result.updatedProtocol.client.id.toString(),
                clientName = result.updatedProtocol.client.name,
                vehicleInfo = "${result.updatedProtocol.vehicle.make} ${result.updatedProtocol.vehicle.model}",
                totalAmount = result.servicesUpdateResult.totalAmount,
                servicesCompleted = result.servicesUpdateResult.serviceNames,
                companyId = user.companyId,
                userId = user.id,
                userName = user.name
            )
        )
    }


    private fun publishStatusChanged(result: ProtocolUpdateResult, user: CurrentUser) {
        eventPublisher.publish(
            ProtocolStatusChangedEvent(
                protocolId = result.protocolId.value,
                oldStatus = result.oldStatus.name,
                newStatus = result.newStatus.name,
                reason = null,
                protocolTitle = result.updatedProtocol.title,
                companyId = user.companyId,
                userId = user.id,
                userName = user.name
            )
        )
    }

    private fun publishGeneralUpdateEvent(result: ProtocolUpdateResult, user: CurrentUser) {
        eventPublisher.publish(
            ProtocolUpdatedEvent(
                protocolId = result.protocolId.value,
                protocolTitle = result.updatedProtocol.title,
                changedFields = result.buildChangedFieldsMap(),
                updateReason = "Protocol data updated",
                companyId = user.companyId,
                userId = user.id,
                userName = user.name
            )
        )
    }
}

data class CurrentUser(
    val id: String,
    val name: String,
    val companyId: Long
)