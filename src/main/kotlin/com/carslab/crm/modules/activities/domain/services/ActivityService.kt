// src/main/kotlin/com/carslab/crm/modules/activities/domain/services/ActivityService.kt
package com.carslab.crm.modules.activities.domain.services

import com.carslab.crm.modules.activities.application.queries.models.*
import com.carslab.crm.modules.activities.infrastructure.persistence.repository.ActivityJpaRepository
import com.carslab.crm.modules.activities.infrastructure.persistence.entity.ActivityEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing activities (CRUD operations for internal use by other modules)
 * This service is NOT exposed via REST API - only for internal module communication
 */
@Service
@Transactional
class ActivityService(
    private val activityJpaRepository: ActivityJpaRepository,
    private val securityContext: SecurityContext
) {

    private val logger = LoggerFactory.getLogger(ActivityService::class.java)

    /**
     * Create a new activity
     */
    fun createActivity(
        category: ActivityCategory,
        message: String,
        entityType: EntityType? = null,
        entityId: String? = null,
        entities: List<ActivityEntityReadModel>? = null,
        status: ActivityStatus? = null,
        statusText: String? = null,
        metadata: ActivityMetadataReadModel? = null,
        userId: String? = null,
        userName: String? = null,
        userColor: String? = null
    ): String {
        val companyId = getCurrentCompanyId()
        val activityId = UUID.randomUUID().toString()
        val timestamp = LocalDateTime.now()

        val currentUserId = userId ?: securityContext.getCurrentUserId()?.toString()
        val currentUserName = userName ?: securityContext.getCurrentUserName()

        try {
            val activityEntity = ActivityEntity.fromDomain(
                activityId = activityId,
                companyId = companyId,
                timestamp = timestamp,
                category = category,
                message = message,
                userId = currentUserId,
                userName = currentUserName,
                userColor = userColor,
                entityType = entityType,
                entityId = entityId,
                entities = entities,
                status = status,
                statusText = statusText,
                metadata = metadata
            )

            activityJpaRepository.save(activityEntity)

            logger.info("Created activity: {} for company: {}", activityId, companyId)
            return activityId

        } catch (e: Exception) {
            logger.error("Failed to create activity for company: {}", companyId, e)
            throw RuntimeException("Failed to create activity", e)
        }
    }

    /**
     * Create appointment-related activity
     */
    fun createAppointmentActivity(
        message: String,
        appointmentId: String,
        appointmentDisplayName: String,
        clientId: String? = null,
        clientDisplayName: String? = null,
        vehicleId: String? = null,
        vehicleDisplayName: String? = null,
        status: ActivityStatus = ActivityStatus.SUCCESS,
        metadata: Map<String, Any>? = null
    ): String {
        val entities = mutableListOf<ActivityEntityReadModel>()

        // Add appointment entity
        entities.add(ActivityEntityReadModel(
            id = appointmentId,
            type = EntityType.APPOINTMENT,
            displayName = appointmentDisplayName
        ))

        // Add client entity if provided
        clientId?.let {
            entities.add(ActivityEntityReadModel(
                id = it,
                type = EntityType.CLIENT,
                displayName = clientDisplayName ?: "Client $it",
                relatedId = appointmentId
            ))
        }

        // Add vehicle entity if provided
        vehicleId?.let {
            entities.add(ActivityEntityReadModel(
                id = it,
                type = EntityType.VEHICLE,
                displayName = vehicleDisplayName ?: "Vehicle $it",
                relatedId = appointmentId
            ))
        }

        val activityMetadata = metadata?.let { meta ->
            ActivityMetadataReadModel(
                appointmentDuration = meta["duration"] as? Int,
                servicesList = meta["services"] as? List<String>,
                notes = meta["notes"] as? String
            )
        }

        return createActivity(
            category = ActivityCategory.APPOINTMENT,
            message = message,
            entityType = EntityType.APPOINTMENT,
            entityId = appointmentId,
            entities = entities,
            status = status,
            metadata = activityMetadata
        )
    }

    /**
     * Create protocol-related activity
     */
    fun createProtocolActivity(
        message: String,
        protocolId: String,
        protocolDisplayName: String,
        clientId: String? = null,
        clientDisplayName: String? = null,
        vehicleId: String? = null,
        vehicleDisplayName: String? = null,
        status: ActivityStatus = ActivityStatus.SUCCESS,
        metadata: Map<String, Any>? = null
    ): String {
        val entities = mutableListOf<ActivityEntityReadModel>()

        entities.add(ActivityEntityReadModel(
            id = protocolId,
            type = EntityType.PROTOCOL,
            displayName = protocolDisplayName
        ))

        clientId?.let {
            entities.add(ActivityEntityReadModel(
                id = it,
                type = EntityType.CLIENT,
                displayName = clientDisplayName ?: "Client $it",
                relatedId = protocolId
            ))
        }

        vehicleId?.let {
            entities.add(ActivityEntityReadModel(
                id = it,
                type = EntityType.VEHICLE,
                displayName = vehicleDisplayName ?: "Vehicle $it",
                relatedId = protocolId
            ))
        }

        val activityMetadata = metadata?.let { meta ->
            ActivityMetadataReadModel(
                vehicleCondition = meta["vehicleCondition"] as? String,
                damageCount = meta["damageCount"] as? Int,
                notes = meta["notes"] as? String,
                previousValue = meta["previousValue"] as? String,
                newValue = meta["newValue"] as? String
            )
        }

        return createActivity(
            category = ActivityCategory.PROTOCOL,
            message = message,
            entityType = EntityType.PROTOCOL,
            entityId = protocolId,
            entities = entities,
            status = status,
            metadata = activityMetadata
        )
    }

    /**
     * Create client-related activity
     */
    fun createClientActivity(
        message: String,
        clientId: String,
        clientDisplayName: String,
        status: ActivityStatus = ActivityStatus.SUCCESS,
        metadata: Map<String, Any>? = null
    ): String {
        val entities = listOf(
            ActivityEntityReadModel(
                id = clientId,
                type = EntityType.CLIENT,
                displayName = clientDisplayName
            )
        )

        val activityMetadata = metadata?.let { meta ->
            ActivityMetadataReadModel(
                notes = meta["notes"] as? String,
                previousValue = meta["previousValue"] as? String,
                newValue = meta["newValue"] as? String
            )
        }

        return createActivity(
            category = ActivityCategory.CLIENT,
            message = message,
            entityType = EntityType.CLIENT,
            entityId = clientId,
            entities = entities,
            status = status,
            metadata = activityMetadata
        )
    }

    /**
     * Create vehicle-related activity
     */
    fun createVehicleActivity(
        message: String,
        vehicleId: String,
        vehicleDisplayName: String,
        status: ActivityStatus = ActivityStatus.SUCCESS,
        metadata: Map<String, Any>? = null
    ): String {
        val entities = listOf(
            ActivityEntityReadModel(
                id = vehicleId,
                type = EntityType.VEHICLE,
                displayName = vehicleDisplayName,
                metadata = metadata?.filterKeys { key ->
                    listOf("vehicleRegistration", "serviceType").contains(key)
                }
            )
        )

        val activityMetadata = metadata?.let { meta ->
            ActivityMetadataReadModel(
                notes = meta["notes"] as? String,
                previousValue = meta["previousValue"] as? String,
                newValue = meta["newValue"] as? String,
                vehicleCondition = meta["vehicleCondition"] as? String
            )
        }

        return createActivity(
            category = ActivityCategory.VEHICLE,
            message = message,
            entityType = EntityType.VEHICLE,
            entityId = vehicleId,
            entities = entities,
            status = status,
            metadata = activityMetadata
        )
    }

    /**
     * Create comment-related activity
     */
    fun createCommentActivity(
        message: String,
        commentId: String,
        commentContent: String,
        parentEntityType: EntityType,
        parentEntityId: String,
        parentEntityDisplayName: String,
        commentType: String = "internal",
        isResolved: Boolean = false
    ): String {
        val entities = listOf(
            ActivityEntityReadModel(
                id = commentId,
                type = EntityType.COMMENT,
                displayName = "Comment: ${commentContent.take(50)}...",
                relatedId = parentEntityId
            ),
            ActivityEntityReadModel(
                id = parentEntityId,
                type = parentEntityType,
                displayName = parentEntityDisplayName
            )
        )

        val activityMetadata = ActivityMetadataReadModel(
            commentType = commentType,
            isResolved = isResolved,
            notes = commentContent
        )

        return createActivity(
            category = ActivityCategory.COMMENT,
            message = message,
            entityType = EntityType.COMMENT,
            entityId = commentId,
            entities = entities,
            status = ActivityStatus.SUCCESS,
            metadata = activityMetadata
        )
    }

    /**
     * Create system notification activity
     */
    fun createNotificationActivity(
        message: String,
        notificationType: String,
        notificationContent: String,
        targetEntityType: EntityType? = null,
        targetEntityId: String? = null,
        targetEntityDisplayName: String? = null
    ): String {
        val entities = if (targetEntityType != null && targetEntityId != null && targetEntityDisplayName != null) {
            listOf(
                ActivityEntityReadModel(
                    id = targetEntityId,
                    type = targetEntityType,
                    displayName = targetEntityDisplayName
                )
            )
        } else null

        val activityMetadata = ActivityMetadataReadModel(
            notificationType = notificationType,
            notificationContent = notificationContent,
            isRead = false
        )

        return createActivity(
            category = ActivityCategory.NOTIFICATION,
            message = message,
            entityType = targetEntityType,
            entityId = targetEntityId,
            entities = entities,
            status = ActivityStatus.SUCCESS,
            metadata = activityMetadata
        )
    }

    /**
     * Create system activity
     */
    fun createSystemActivity(
        message: String,
        systemAction: String,
        affectedRecords: Int = 0,
        metadata: Map<String, Any>? = null
    ): String {
        val activityMetadata = ActivityMetadataReadModel(
            systemAction = systemAction,
            affectedRecords = affectedRecords,
            notes = metadata?.get("notes") as? String
        )

        return createActivity(
            category = ActivityCategory.SYSTEM,
            message = message,
            status = ActivityStatus.SUCCESS,
            metadata = activityMetadata
        )
    }

    /**
     * Update activity status
     */
    fun updateActivityStatus(
        activityId: String,
        status: ActivityStatus,
        statusText: String? = null
    ): Boolean {
        val companyId = getCurrentCompanyId()

        return try {
            val activity = activityJpaRepository.findByActivityIdAndCompanyId(activityId, companyId)
            if (activity != null) {
                val updatedActivity = ActivityEntity.fromDomain(
                    activityId = activity.activityId,
                    companyId = activity.companyId,
                    timestamp = activity.timestamp,
                    category = activity.category,
                    message = activity.message,
                    userId = activity.userId,
                    userName = activity.userName,
                    userColor = activity.userColor,
                    entityType = activity.entityType,
                    entityId = activity.entityId,
                    entities = activity.toReadModel().entities,
                    status = status,
                    statusText = statusText,
                    metadata = activity.toReadModel().metadata
                )

                activityJpaRepository.save(updatedActivity)
                logger.info("Updated activity status: {} to {}", activityId, status)
                true
            } else {
                logger.warn("Activity not found for update: {}", activityId)
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to update activity status: {}", activityId, e)
            false
        }
    }

    /**
     * Delete activity (soft delete by marking as system action)
     */
    fun deleteActivity(activityId: String): Boolean {
        val companyId = getCurrentCompanyId()

        return try {
            val activity = activityJpaRepository.findByActivityIdAndCompanyId(activityId, companyId)
            if (activity != null) {
                activityJpaRepository.delete(activity)
                logger.info("Deleted activity: {}", activityId)

                // Create system activity about deletion
                createSystemActivity(
                    message = "Activity deleted: ${activity.message}",
                    systemAction = "DELETE_ACTIVITY",
                    affectedRecords = 1
                )

                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to delete activity: {}", activityId, e)
            false
        }
    }

    /**
     * Cleanup old activities (for maintenance)
     */
    fun cleanupOldActivities(olderThanDays: Int = 365): Int {
        val companyId = getCurrentCompanyId()
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())

        return try {
            val deletedCount = activityJpaRepository.deleteOldActivities(companyId, cutoffDate)
            logger.info("Cleaned up {} old activities for company: {}", deletedCount, companyId)

            if (deletedCount > 0) {
                createSystemActivity(
                    message = "Cleaned up old activities",
                    systemAction = "CLEANUP_ACTIVITIES",
                    affectedRecords = deletedCount
                )
            }

            deletedCount
        } catch (e: Exception) {
            logger.error("Failed to cleanup old activities for company: {}", companyId, e)
            0
        }
    }

    /**
     * Check if activity exists
     */
    fun activityExists(activityId: String): Boolean {
        val companyId = getCurrentCompanyId()
        return activityJpaRepository.existsByActivityIdAndCompanyId(activityId, companyId)
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }
}