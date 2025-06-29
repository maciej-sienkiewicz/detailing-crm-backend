package com.carslab.crm.modules.visits.infrastructure.events

import com.carslab.crm.modules.activities.domain.services.ActivityService
import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.visits.domain.events.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class VisitCommentActivityEventHandler(
    private val activityService: ActivityService
) {
    private val logger = LoggerFactory.getLogger(VisitCommentActivityEventHandler::class.java)

    @Async
    @EventListener
    fun handleVisitCommentAdded(event: VisitCommentAddedEvent) {
        logger.debug("Handling visit comment added event: ${event.visitId}")

        try {
            val entities = listOf(
                ActivityEntityReadModel(
                    id = event.visitId,
                    type = EntityType.PROTOCOL,
                    displayName = event.protocolTitle ?: "Visit ${event.visitId}"
                )
            )

            val commentTypeText = when (event.type) {
                "internal" -> "komentarz wewnętrzny"
                "customer" -> "komentarz klienta"
                "system" -> "komentarz systemowy"
                else -> "komentarz"
            }

            activityService.createActivity(
                category = ActivityCategory.COMMENT,
                message = "Dodano $commentTypeText do wizyty",
                entityType = EntityType.COMMENT,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Autor: ${event.author}, Treść: ${truncateContent(event.content)}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit comment added event: ${event.visitId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVisitCommentUpdated(event: VisitCommentUpdatedEvent) {
        logger.debug("Handling visit comment updated event: ${event.commentId}")

        try {
            val entities = listOf(
                ActivityEntityReadModel(
                    id = event.visitId,
                    type = EntityType.PROTOCOL,
                    displayName = "Visit ${event.visitId}"
                )
            )

            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Zaktualizowano komentarz w wizycie",
                entityType = EntityType.PROTOCOL,
                entityId = event.visitId,
                entities = entities,
                status = ActivityStatus.SUCCESS,
                metadata = ActivityMetadataReadModel(
                    notes = "Nowa treść: ${truncateContent(event.newContent)}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit comment updated event: ${event.commentId}", e)
        }
    }

    @Async
    @EventListener
    fun handleVisitCommentDeleted(event: VisitCommentDeletedEvent) {
        logger.debug("Handling visit comment deleted event: ${event.commentId}")

        try {
            activityService.createActivity(
                category = ActivityCategory.PROTOCOL,
                message = "Usunięto komentarz",
                entityType = EntityType.COMMENT,
                entityId = event.commentId,
                status = ActivityStatus.ERROR,
                statusText = "Komentarz usunięty",
                metadata = ActivityMetadataReadModel(
                    notes = "Usunięto komentarz ID: ${event.commentId}"
                ),
                userId = event.userId,
                userName = event.userName
            )
        } catch (e: Exception) {
            logger.error("Failed to create activity for visit comment deleted event: ${event.commentId}", e)
        }
    }

    private fun truncateContent(content: String, maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content
        } else {
            "${content.take(maxLength)}..."
        }
    }
}