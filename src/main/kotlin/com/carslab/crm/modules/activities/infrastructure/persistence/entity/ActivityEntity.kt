// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/persistence/entity/ActivityEntity.kt
package com.carslab.crm.modules.activities.infrastructure.persistence.entity

import com.carslab.crm.modules.activities.application.queries.models.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "activities",
    indexes = [
        Index(name = "idx_activities_company_timestamp", columnList = "companyId,timestamp"),
        Index(name = "idx_activities_category", columnList = "category"),
        Index(name = "idx_activities_entity", columnList = "entityType,entityId"),
        Index(name = "idx_activities_user", columnList = "userId"),
        Index(name = "idx_activities_status", columnList = "status"),
        Index(name = "idx_activities_date_range", columnList = "companyId,timestamp,category")
    ]
)
class ActivityEntity(
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

    @Transient
    private val objectMapper = ObjectMapper()

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
                val typeRef = object : TypeReference<List<Map<String, Any>>>() {}
                val entitiesList = objectMapper.readValue(json, typeRef)
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
                null
            }
        }
    }

    private fun parseMetadata(): ActivityMetadataReadModel? {
        return metadataJson?.let { json ->
            try {
                val metadataMap = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
                ActivityMetadataReadModel(
                    notes = metadataMap["notes"] as String?,
                    previousValue = metadataMap["previousValue"] as String?,
                    newValue = metadataMap["newValue"] as String?,
                    appointmentDuration = metadataMap["appointmentDuration"] as Int?,
                    servicesList = metadataMap["servicesList"] as List<String>?,
                    vehicleCondition = metadataMap["vehicleCondition"] as String?,
                    damageCount = metadataMap["damageCount"] as Int?,
                    commentType = metadataMap["commentType"] as String?,
                    isResolved = metadataMap["isResolved"] as Boolean?,
                    notificationType = metadataMap["notificationType"] as String?,
                    notificationContent = metadataMap["notificationContent"] as String?,
                    isRead = metadataMap["isRead"] as Boolean?,
                    systemAction = metadataMap["systemAction"] as String?,
                    affectedRecords = metadataMap["affectedRecords"] as Int?
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private val objectMapper = ObjectMapper()

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
        ): ActivityEntity {

            val entitiesJson = entities?.let { entitiesList ->
                try {
                    val entitiesMap = entitiesList.map { entity ->
                        mapOf(
                            "id" to entity.id,
                            "type" to entity.type.name,
                            "displayName" to entity.displayName,
                            "relatedId" to entity.relatedId,
                            "metadata" to entity.metadata
                        )
                    }
                    objectMapper.writeValueAsString(entitiesMap)
                } catch (e: Exception) {
                    null
                }
            }

            val metadataJson = metadata?.let { meta ->
                try {
                    val metadataMap = mapOf(
                        "notes" to meta.notes,
                        "previousValue" to meta.previousValue,
                        "newValue" to meta.newValue,
                        "appointmentDuration" to meta.appointmentDuration,
                        "servicesList" to meta.servicesList,
                        "vehicleCondition" to meta.vehicleCondition,
                        "damageCount" to meta.damageCount,
                        "commentType" to meta.commentType,
                        "isResolved" to meta.isResolved,
                        "notificationType" to meta.notificationType,
                        "notificationContent" to meta.notificationContent,
                        "isRead" to meta.isRead,
                        "systemAction" to meta.systemAction,
                        "affectedRecords" to meta.affectedRecords
                    ).filterValues { it != null }
                    objectMapper.writeValueAsString(metadataMap)
                } catch (e: Exception) {
                    null
                }
            }

            return ActivityEntity(
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