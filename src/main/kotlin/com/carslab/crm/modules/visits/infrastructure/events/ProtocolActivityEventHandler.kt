package com.carslab.crm.modules.visits.infrastructure.events

import com.carslab.crm.modules.activities.domain.services.ActivityService
import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.visits.domain.events.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Enhanced event handler that creates activities from protocol events
 * This provides comprehensive audit trail for all protocol operations
 */
@Component
class ProtocolActivityEventHandler(
    private val activityService: ActivityService
) {
    private val logger = LoggerFactory.getLogger(ProtocolActivityEventHandler::class.java)

    @Async
    @EventListener
    fun handleProtocolCreated(event: ProtocolCreatedEvent) {
        logger.debug("Handling protocol created event: ${event.protocolId}")

        try {
            val entities = buildProtocolEntities(
                protocolId = event.protocolId,
                protocolTitle = event.protocolTitle,
                clientId = event.clientId,
                clientName = event.clientName,
                vehicleId = event.vehicleId,
                vehicleMake = event.vehicleMake,
                vehicleModel = event.vehicleModel,
                licensePlate = event.licensePlate
            )

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Utworzono nowy protokół: ${event.protocolTitle}",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Status: ${event.status}, Usługi: ${event.servicesCount}, Kwota: ${event.totalAmount} PLN"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for protocol created event: ${event.protocolId}", e)
        }
    }

    @Async
    @EventListener
    fun handleProtocolStatusChanged(event: ProtocolStatusChangedEvent) {
        logger.debug("Handling protocol status changed event: ${event.protocolId}")

        try {
            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Zmieniono status protokołu z \"${mapStatusToPolish(event.oldStatus)}\" na \"${mapStatusToPolish(event.newStatus)}\"",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                status = when (event.newStatus) {
                    "COMPLETED" -> ActivityStatus.SUCCESS
                    "CANCELLED" -> ActivityStatus.ERROR
                    else -> ActivityStatus.SUCCESS
                },
                metadata = ActivityMetadataReadModel(
                    previousValue = event.oldStatus,
                    newValue = event.newStatus,
                    notes = event.reason,
                ),
                userId = event.userId,
                userName = event.userName,
                entities = listOf(ActivityEntityReadModel(
                    id = event.protocolId,
                    type = EntityType.PROTOCOL,
                    displayName = event.protocolTitle ?: ""
                ))
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for protocol status changed event: ${event.protocolId}", e)
        }
    }
    
    @Async
    @EventListener
    fun handleProtocolDocumentUploaded(event: ProtocolDocumentUploadedEvent) {
        logger.debug("Handling protocol document uploaded event: ${event.protocolId}")

        try {
            activityService.createActivity(
                category = ActivityCategory.DOCUMENT,
                message = "Dodano dokument: ${event.documentType.displayName}",
                entityType = EntityType.DOCUMENT,
                entityId = event.documentId,
                entities = listOf(
                    ActivityEntityReadModel(
                        id = event.protocolId,
                        type = EntityType.PROTOCOL,
                        displayName = event.originalName,
                        metadata = mapOf("fileSize" to formatFileSize(event.fileSize))
                    )
                ),
                status = ActivityStatus.SUCCESS,
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for document uploaded event: ${event.protocolId}", e)
        }
    }

    @Async
    @EventListener
    fun handleProtocolDocumentDeleted(event: ProtocolDocumentDeletedEvent) {
        logger.debug("Handling protocol document deleted event: ${event.protocolId}")

        try {
            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Usunięto dokument: ${event.originalName}",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Typ: ${event.documentType}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for document deleted event: ${event.protocolId}", e)
        }
    }

    @Async
    @EventListener
    fun handleProtocolWorkStarted(event: ProtocolWorkStartedEvent) {
        logger.debug("Handling protocol work started event: ${event.protocolId}")

        val entities = listOf(
            ActivityEntityReadModel(
                id = event.protocolId,
                type = EntityType.APPOINTMENT,
                displayName = event.protocolTitle,
            ),
            ActivityEntityReadModel(
                id = event.clientId,
                type = EntityType.CLIENT,
                displayName = event.clientName,
                relatedId = event.protocolId
            ),
            ActivityEntityReadModel(
                id = event.vehicleId,
                type = EntityType.VEHICLE,
                displayName = event.vehicleDisplayName,
                relatedId = event.protocolId,
            )
        )

        try {
            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Przyjęto nowy pojazd. Tytuł wizyty: ${event.protocolTitle}",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                status = ActivityStatus.SUCCESS,
                entities = entities,
                metadata = ActivityMetadataReadModel(
                    servicesList = event.plannedServices
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for work started event: ${event.protocolId}", e)
        }
    }

    @Async
    @EventListener
    fun handleProtocolDeleted(event: ProtocolDeletedEvent) {
        logger.debug("Handling protocol deleted event: ${event.protocolId}")

        try {
            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Usunięto protokół: ${event.protocolTitle}",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                status = ActivityStatus.ERROR,
                statusText = "Protokół usunięty",
                metadata = ActivityMetadataReadModel(
                    notes = "Klient: ${event.clientName}, Pojazd: ${event.vehicleInfo}, Powód: ${event.deletionReason}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for protocol deleted event: ${event.protocolId}", e)
        }
    }

    private fun buildProtocolEntities(
        protocolId: String,
        protocolTitle: String,
        clientId: String?,
        clientName: String?,
        vehicleId: String?,
        vehicleMake: String?,
        vehicleModel: String?,
        licensePlate: String?
    ): List<ActivityEntityReadModel> {
        val entities = mutableListOf<ActivityEntityReadModel>()

        // Always add the protocol entity
        entities.add(ActivityEntityReadModel(
            id = protocolId,
            type = EntityType.PROTOCOL,
            displayName = protocolTitle
        ))

        // Add client entity if available
        if (clientId != null && clientName != null) {
            entities.add(ActivityEntityReadModel(
                id = clientId,
                type = EntityType.CLIENT,
                displayName = clientName,
                relatedId = protocolId
            ))
        }

        // Add vehicle entity if available
        if (vehicleId != null && vehicleMake != null && vehicleModel != null) {
            entities.add(ActivityEntityReadModel(
                id = vehicleId,
                type = EntityType.VEHICLE,
                displayName = "$vehicleMake $vehicleModel${if (licensePlate != null) " ($licensePlate)" else ""}",
                relatedId = protocolId
            ))
        }
    
        return entities
    }

    private fun mapStatusToPolish(status: String): String {
        return when (status.uppercase()) {
            "SCHEDULED" -> "Zaplanowany"
            "IN_PROGRESS" -> "W trakcie"
            "READY_FOR_PICKUP" -> "Gotowy do odbioru"
            "COMPLETED" -> "Zakończony"
            "CANCELLED" -> "Anulowany"
            "PENDING_APPROVAL" -> "Oczekuje na zatwierdzenie"
            else -> status
        }
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> "${sizeInBytes / (1024 * 1024)} MB"
        }
    }
}