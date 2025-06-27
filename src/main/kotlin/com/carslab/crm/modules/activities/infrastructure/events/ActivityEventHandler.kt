// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/events/ActivityEventHandler.kt
package com.carslab.crm.modules.activities.infrastructure.events

import com.carslab.crm.infrastructure.events.InvoiceCreatedEvent
import com.carslab.crm.infrastructure.events.InvoiceDeletedEvent
import com.carslab.crm.infrastructure.events.InvoiceEditedEvent
import com.carslab.crm.infrastructure.events.InvoiceStatusChangedEvent
import com.carslab.crm.modules.activities.domain.services.ActivityService
import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.visits.domain.events.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Handler for converting domain events into activities
 * This class acts as a bridge between domain events and activity creation
 */
@Component
class ActivityEventHandler(
    private val activityService: ActivityService
) {

    private val logger = LoggerFactory.getLogger(ActivityEventHandler::class.java)

    // =================== VISIT EVENTS ===================

    @Async
    @EventListener
    fun handleVisitScheduled(event: VisitScheduledEvent) {
        logger.debug("Handling visit scheduled event: {}", event.visitId)

        try {
            val entities = buildVisitEntities(
                visitId = event.visitId,
                visitTitle = event.visitTitle,
                clientId = event.clientId,
                clientName = event.clientName,
                vehicleId = event.vehicleId,
                vehicleDisplayName = "${event.vehicleMake ?: ""} ${event.vehicleModel ?: ""} (${event.licensePlate ?: ""})".trim()
            )

            activityService.createActivity(
                category = ActivityCategory.APPOINTMENT,
                message = "Zaplanowano wizytę: ${event.visitTitle}",
                entityType = EntityType.APPOINTMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    appointmentDuration = null, // Calculate if available
                    servicesList = event.services,
                    notes = "Wizyta zaplanowana na ${event.scheduledDate}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit scheduled event: ${event.visitId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVisitStarted(event: VisitStartedEvent) {
        logger.debug("Handling visit started event: {}", event.visitId)

        try {
            val entities = buildVisitEntities(
                visitId = event.visitId,
                visitTitle = event.visitTitle,
                clientId = event.clientId,
                clientName = event.clientName,
                vehicleId = event.vehicleId,
                vehicleDisplayName = event.vehicleDisplayName
            )

            activityService.createActivity(
                category = ActivityCategory.APPOINTMENT,
                message = "Rozpoczęto wizytę: ${event.visitTitle}",
                entityType = EntityType.APPOINTMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Wizyta rozpoczęta o ${event.startedAt}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit started event: ${event.visitId}", e)
        }
    }

    @Async
    @EventListener
    fun handleProtocolEdited(event: ProtocolEditedEvent) {
        logger.debug("Handling protocol edited event: {}", event.protocolId)

        try {
            val entities = mutableListOf<ActivityEntityReadModel>()
            entities.add(ActivityEntityReadModel(
                id = event.protocolId,
                type = EntityType.PROTOCOL,
                displayName = event.protocolTitle
            ))

            event.visitId?.let { visitId ->
                entities.add(ActivityEntityReadModel(
                    id = visitId,
                    type = EntityType.APPOINTMENT,
                    displayName = "Wizyta",
                    relatedId = event.protocolId
                ))
            }

            val changesDescription = event.changedFields.map { (field, values) ->
                "$field: '${values.first ?: "brak"}' → '${values.second ?: "brak"}'"
            }.joinToString(", ")

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Edytowano protokół: ${event.protocolTitle}",
                entityType = EntityType.PROTOCOL,
                entityId = event.protocolId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Zmienione pola: $changesDescription",
                    previousValue = event.changedFields.toString(),
                    newValue = "Protokół zaktualizowany"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for protocol edited event: ${event.protocolId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVehicleReleased(event: VehicleReleasedEvent) {
        logger.debug("Handling vehicle released event: {}", event.visitId)

        try {
            val entities = listOf(
                ActivityEntityReadModel(
                    id = event.visitId,
                    type = EntityType.APPOINTMENT,
                    displayName = "Wizyta"
                ),
                ActivityEntityReadModel(
                    id = event.protocolId,
                    type = EntityType.PROTOCOL,
                    displayName = "Protokół",
                    relatedId = event.visitId
                ),
                ActivityEntityReadModel(
                    id = event.clientId,
                    type = EntityType.CLIENT,
                    displayName = event.clientName,
                    relatedId = event.visitId
                ),
                ActivityEntityReadModel(
                    id = event.vehicleId,
                    type = EntityType.VEHICLE,
                    displayName = event.vehicleDisplayName,
                    relatedId = event.visitId,
                    metadata = mapOf(
                        "paymentMethod" to event.paymentMethod,
                        "totalAmount" to (event.totalAmount ?: 0.0)
                    )
                )
            )

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Oddano pojazd klientowi: ${event.clientName}",
                entityType = EntityType.APPOINTMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Pojazd: ${event.vehicleDisplayName}, Płatność: ${event.paymentMethod}" +
                            if (event.totalAmount != null) ", Kwota: ${event.totalAmount} PLN" else "" +
                                    if (event.releaseNotes != null) ", Uwagi: ${event.releaseNotes}" else ""
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for vehicle released event: ${event.visitId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVisitMediaChanged(event: VisitMediaChangedEvent) {
        logger.debug("Handling visit media changed event: {} - {}", event.visitId, event.action)

        try {
            val entities = mutableListOf<ActivityEntityReadModel>()
            entities.add(ActivityEntityReadModel(
                id = event.visitId,
                type = EntityType.APPOINTMENT,
                displayName = "Wizyta"
            ))

            event.vehicleId?.let { vehicleId ->
                entities.add(ActivityEntityReadModel(
                    id = vehicleId,
                    type = EntityType.VEHICLE,
                    displayName = event.vehicleDisplayName ?: "Pojazd",
                    relatedId = event.visitId
                ))
            }

            val actionMessage = when (event.action) {
                MediaAction.ADD -> "Dodano zdjęcie"
                MediaAction.REMOVE -> "Usunięto zdjęcie"
                MediaAction.UPDATE -> "Zaktualizowano zdjęcie"
            }

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "$actionMessage: ${event.mediaName}",
                entityType = EntityType.APPOINTMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Operacja na pliku: ${event.mediaName}" +
                            if (event.mediaSize != null) " (${event.mediaSize} bytes)" else ""
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit media changed event: ${event.visitId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVisitCancelled(event: VisitCancelledEvent) {
        logger.debug("Handling visit cancelled event: {}", event.visitId)

        try {
            val entities = buildVisitEntities(
                visitId = event.visitId,
                visitTitle = event.visitTitle,
                clientId = event.clientId,
                clientName = event.clientName,
                vehicleId = event.vehicleId,
                vehicleDisplayName = event.vehicleDisplayName
            )

            activityService.createActivity(
                category = ActivityCategory.APPOINTMENT,
                message = "Anulowano wizytę: ${event.visitTitle}",
                entityType = EntityType.APPOINTMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.ERROR,
                statusText = "Wizyta anulowana",
                metadata = ActivityMetadataReadModel(
                    notes = "Powód anulowania: ${event.cancellationReason}. " +
                            "Planowana data: ${event.scheduledDate}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit cancelled event: ${event.visitId}", e)
        }
    }

    // =================== INVOICE EVENTS ===================

    @Async
    @EventListener
    fun handleInvoiceCreated(event: InvoiceCreatedEvent) {
        logger.debug("Handling invoice created event: {}", event.invoiceId)

        try {
            val entities = buildInvoiceEntities(
                invoiceId = event.invoiceId,
                invoiceNumber = event.invoiceNumber,
                clientId = event.clientId,
                clientName = event.clientName,
                visitId = event.visitId,
                visitTitle = event.visitTitle
            )

            activityService.createActivity(
                category = ActivityCategory.SYSTEM,
                message = "Utworzono fakturę: ${event.invoiceNumber}",
                entityType = EntityType.INVOICE,
                entityId = event.invoiceId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Kwota: ${event.amount} ${event.currency}" +
                            if (event.dueDate != null) ", Termin płatności: ${event.dueDate}" else ""
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for invoice created event: ${event.invoiceId}", e)
        }
    }

    @Async
    @EventListener
    fun handleInvoiceStatusChanged(event: InvoiceStatusChangedEvent) {
        logger.debug("Handling invoice status changed event: {}", event.invoiceId)

        try {
            val entities = buildInvoiceEntities(
                invoiceId = event.invoiceId,
                invoiceNumber = event.invoiceNumber,
                clientId = event.clientId,
                clientName = event.clientName,
                visitId = event.visitId,
                visitTitle = null
            )

            val statusMessage = when (event.newStatus.uppercase()) {
                "PAID" -> "Faktura opłacona"
                "OVERDUE" -> "Faktura przeterminowana"
                "CANCELLED" -> "Faktura anulowana"
                "SENT" -> "Faktura wysłana"
                else -> "Zmieniono status faktury"
            }

            val activityStatus = when (event.newStatus.uppercase()) {
                "PAID" -> ActivityStatus.SUCCESS
                "OVERDUE" -> ActivityStatus.ERROR
                "CANCELLED" -> ActivityStatus.ERROR
                else -> ActivityStatus.SUCCESS
            }

            activityService.createActivity(
                category = ActivityCategory.SYSTEM,
                message = "$statusMessage: ${event.invoiceNumber}",
                entityType = EntityType.INVOICE,
                entityId = event.invoiceId,
                entities = entities,
                status = activityStatus,
                statusText = event.newStatus,
                metadata = ActivityMetadataReadModel(
                    previousValue = event.oldStatus,
                    newValue = event.newStatus,
                    notes = event.statusChangeReason ?: "Zmiana statusu faktury"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for invoice status changed event: ${event.invoiceId}", e)
        }
    }

    @Async
    @EventListener
    fun handleInvoiceEdited(event: InvoiceEditedEvent) {
        logger.debug("Handling invoice edited event: {}", event.invoiceId)

        try {
            val entities = buildInvoiceEntities(
                invoiceId = event.invoiceId,
                invoiceNumber = event.invoiceNumber,
                clientId = event.clientId,
                clientName = null,
                visitId = event.visitId,
                visitTitle = null
            )

            val changesDescription = event.changedFields.map { (field, values) ->
                "$field: '${values.first ?: "brak"}' → '${values.second ?: "brak"}'"
            }.joinToString(", ")

            activityService.createActivity(
                category = ActivityCategory.SYSTEM,
                message = "Edytowano fakturę: ${event.invoiceNumber}",
                entityType = EntityType.INVOICE,
                entityId = event.invoiceId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Zmienione pola: $changesDescription",
                    previousValue = event.changedFields.toString(),
                    newValue = "Faktura zaktualizowana"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for invoice edited event: ${event.invoiceId}", e)
        }
    }

    @Async
    @EventListener
    fun handleInvoiceDeleted(event: InvoiceDeletedEvent) {
        logger.debug("Handling invoice deleted event: {}", event.invoiceId)

        try {
            val entities = buildInvoiceEntities(
                invoiceId = event.invoiceId,
                invoiceNumber = event.invoiceNumber,
                clientId = event.clientId,
                clientName = event.clientName,
                visitId = event.visitId,
                visitTitle = null
            )

            activityService.createActivity(
                category = ActivityCategory.SYSTEM,
                message = "Usunięto fakturę: ${event.invoiceNumber}",
                entityType = EntityType.INVOICE,
                entityId = event.invoiceId,
                entities = entities,
                status = ActivityStatus.ERROR,
                statusText = "Faktura usunięta",
                metadata = ActivityMetadataReadModel(
                    notes = "Powód usunięcia: ${event.deletionReason}. " +
                            "Kwota: ${event.amount} PLN"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for invoice deleted event: ${event.invoiceId}", e)
        }
    }

    // =================== HELPER METHODS ===================

    private fun buildVisitEntities(
        visitId: String,
        visitTitle: String,
        clientId: String?,
        clientName: String?,
        vehicleId: String?,
        vehicleDisplayName: String?
    ): List<ActivityEntityReadModel> {
        val entities = mutableListOf<ActivityEntityReadModel>()

        // Always add the visit entity
        entities.add(ActivityEntityReadModel(
            id = visitId,
            type = EntityType.APPOINTMENT,
            displayName = visitTitle
        ))

        // Add client entity if available
        if (clientId != null && clientName != null) {
            entities.add(ActivityEntityReadModel(
                id = clientId,
                type = EntityType.CLIENT,
                displayName = clientName,
                relatedId = visitId
            ))
        }

        // Add vehicle entity if available
        if (vehicleId != null && vehicleDisplayName != null) {
            entities.add(ActivityEntityReadModel(
                id = vehicleId,
                type = EntityType.VEHICLE,
                displayName = vehicleDisplayName,
                relatedId = visitId
            ))
        }

        return entities
    }

    private fun buildInvoiceEntities(
        invoiceId: String,
        invoiceNumber: String,
        clientId: String?,
        clientName: String?,
        visitId: String?,
        visitTitle: String?
    ): List<ActivityEntityReadModel> {
        val entities = mutableListOf<ActivityEntityReadModel>()

        // Always add the invoice entity
        entities.add(ActivityEntityReadModel(
            id = invoiceId,
            type = EntityType.INVOICE,
            displayName = invoiceNumber
        ))

        // Add client entity if available
        if (clientId != null && clientName != null) {
            entities.add(ActivityEntityReadModel(
                id = clientId,
                type = EntityType.CLIENT,
                displayName = clientName,
                relatedId = invoiceId
            ))
        }

        // Add visit entity if available
        if (visitId != null) {
            entities.add(ActivityEntityReadModel(
                id = visitId,
                type = EntityType.APPOINTMENT,
                displayName = visitTitle ?: "Wizyta",
                relatedId = invoiceId
            ))
        }

        return entities
    }
}