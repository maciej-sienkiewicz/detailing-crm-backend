// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/persistence/entity/ActivityEntity.kt
package com.carslab.crm.modules.activities.infrastructure.persistence.entity

import com.carslab.crm.modules.activities.application.queries.models.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "activities_deprecated",
    indexes = [
        Index(name = "idx_activities_company_timestamp", columnList = "companyId,timestamp"),
        Index(name = "idx_activities_category", columnList = "category"),
        Index(name = "idx_activities_entity", columnList = "entityType,entityId"),
        Index(name = "idx_activities_user", columnList = "userId"),
        Index(name = "idx_activities_status", columnList = "status"),
        Index(name = "idx_activities_date_range", columnList = "companyId,timestamp,category")
    ]
)
class ActivityEntityDeprecated(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, unique = true)
    val activityId: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: ActivityCategory,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(nullable = true)
    val userId: String? = null,

    @Column(nullable = true)
    val userName: String? = null,

    @Column(nullable = true)
    val userColor: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val entityType: EntityType? = null,

    @Column(nullable = true)
    val entityId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val status: ActivityStatus? = null,

    @Column(nullable = true)
    val statusText: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val entitiesJson: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val metadataJson: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun toReadModel(): ActivityReadModel {
        return ActivityReadModel(
            id = activityId,
            timestamp = timestamp,
            category = category,
            message = message,
            userId = userId,
            userName = userName,
            userColor = userColor,
            entityType = entityType,
            entityId = entityId,
            entities = parseEntities(),
            status = status,
            statusText = statusText,
            metadata = parseMetadata(),
            companyId = companyId
        )
    }

    private fun parseEntities(): List<ActivityEntityReadModel>? {
        return entitiesJson?.let { json ->
            try {
                // Deserializuj jako listę map
                val entitiesList = objectMapper.readValue(json, object : TypeReference<List<Map<String, Any?>>>() {})

                entitiesList.map { entityMap ->
                    ActivityEntityReadModel(
                        id = entityMap["id"] as String,
                        type = EntityType.valueOf(entityMap["type"] as String),
                        displayName = entityMap["displayName"] as String,
                        relatedId = entityMap["relatedId"] as String?,
                        metadata = entityMap["metadata"] as Map<String, Any>?
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to parse entities JSON for activity: $activityId", e)
                null
            }
        }
    }

    private fun parseMetadata(): ActivityMetadataReadModel? {
        return metadataJson?.let { json ->
            try {
                val metadataMap = objectMapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})

                ActivityMetadataReadModel(
                    notes = metadataMap["notes"] as String?,
                    previousValue = metadataMap["previousValue"] as String?,
                    newValue = metadataMap["newValue"] as String?,
                    appointmentDuration = (metadataMap["appointmentDuration"] as? Number)?.toInt(),
                    servicesList = metadataMap["servicesList"] as List<String>?,
                    vehicleCondition = metadataMap["vehicleCondition"] as String?,
                    damageCount = (metadataMap["damageCount"] as? Number)?.toInt(),
                    commentType = metadataMap["commentType"] as String?,
                    isResolved = metadataMap["isResolved"] as Boolean?,
                    notificationType = metadataMap["notificationType"] as String?,
                    notificationContent = metadataMap["notificationContent"] as String?,
                    isRead = metadataMap["isRead"] as Boolean?,
                    systemAction = metadataMap["systemAction"] as String?,
                    affectedRecords = (metadataMap["affectedRecords"] as? Number)?.toInt()
                )
            } catch (e: Exception) {
                logger.error("Failed to parse metadata JSON for activity: $activityId", e)
                null
            }
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val logger = org.slf4j.LoggerFactory.getLogger(ActivityEntityDeprecated::class.java)

        fun fromDomain(
            activityId: String,
            companyId: Long,
            timestamp: LocalDateTime,
            category: ActivityCategory,
            message: String,
            userId: String? = null,
            userName: String? = null,
            userColor: String? = null,
            entityType: EntityType? = null,
            entityId: String? = null,
            entities: List<ActivityEntityReadModel>? = null,
            status: ActivityStatus? = null,
            statusText: String? = null,
            metadata: ActivityMetadataReadModel? = null
        ): ActivityEntityDeprecated {

            val entitiesJson = entities?.let { entitiesList ->
                try {
                    // Serializuj jako listę map z null-safe casting
                    val entitiesMap = entitiesList.map { entity ->
                        mutableMapOf<String, Any?>().apply {
                            put("id", entity.id)
                            put("type", entity.type.name)
                            put("displayName", entity.displayName)
                            put("relatedId", entity.relatedId)
                            put("metadata", entity.metadata)
                        }.filterValues { it != null } // Usuń null values
                    }
                    objectMapper.writeValueAsString(entitiesMap)
                } catch (e: Exception) {
                    logger.error("Failed to serialize entities for activity: $activityId", e)
                    null
                }
            }

            val metadataJson = metadata?.let { meta ->
                try {
                    val metadataMap = mutableMapOf<String, Any?>().apply {
                        put("notes", meta.notes)
                        put("previousValue", meta.previousValue)
                        put("newValue", meta.newValue)
                        put("appointmentDuration", meta.appointmentDuration)
                        put("servicesList", meta.servicesList)
                        put("vehicleCondition", meta.vehicleCondition)
                        put("damageCount", meta.damageCount)
                        put("commentType", meta.commentType)
                        put("isResolved", meta.isResolved)
                        put("notificationType", meta.notificationType)
                        put("notificationContent", meta.notificationContent)
                        put("isRead", meta.isRead)
                        put("systemAction", meta.systemAction)
                        put("affectedRecords", meta.affectedRecords)
                    }.filterValues { it != null } // Usuń null values

                    objectMapper.writeValueAsString(metadataMap)
                } catch (e: Exception) {
                    logger.error("Failed to serialize metadata for activity: $activityId", e)
                    null
                }
            }

            return ActivityEntityDeprecated(
                activityId = activityId,
                companyId = companyId,
                timestamp = timestamp,
                category = category,
                message = message,
                userId = userId,
                userName = userName,
                userColor = userColor,
                entityType = entityType,
                entityId = entityId,
                status = status,
                statusText = statusText,
                entitiesJson = entitiesJson,
                metadataJson = metadataJson
            )
        }
    }
}