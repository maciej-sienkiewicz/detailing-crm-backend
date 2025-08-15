// src/main/kotlin/com/carslab/crm/production/modules/activities/application/dto/ActivityDto.kt
package com.carslab.crm.production.modules.activities.application.dto

import com.carslab.crm.production.modules.activities.domain.model.Activity
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelation
import com.carslab.crm.production.modules.activities.domain.model.ActivityStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.format.DateTimeFormatter

data class RelatedEntityDto(
    val id: String,
    val type: String,
    val name: String
)

data class ActivityResponse(
    val id: String,
    val timestamp: String,
    val category: ActivityCategory,
    val message: String,
    @JsonProperty("user_id")
    val userId: String?,
    @JsonProperty("user_name")
    val userName: String?,
    val status: ActivityStatus?,
    @JsonProperty("status_text")
    val statusText: String?,
    @JsonProperty("primary_entity")
    val primaryEntity: RelatedEntityDto?,
    @JsonProperty("related_entities")
    val relatedEntities: List<RelatedEntityDto>,
    val metadata: Map<String, Any>?
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        fun from(activity: Activity, relatedEntities: List<ActivityEntityRelation> = emptyList()): ActivityResponse {
            return ActivityResponse(
                id = activity.id.value,
                timestamp = activity.timestamp.format(dateTimeFormatter),
                category = activity.category,
                message = activity.message,
                userId = activity.userId,
                userName = activity.userName,
                status = activity.status,
                statusText = activity.statusText,
                primaryEntity = if (activity.primaryEntityId != null && activity.primaryEntityType != null && activity.primaryEntityName != null) {
                    RelatedEntityDto(activity.primaryEntityId, activity.primaryEntityType, activity.primaryEntityName)
                } else null,
                relatedEntities = relatedEntities.map {
                    RelatedEntityDto(it.entityId, it.entityType, it.entityName)
                },
                metadata = activity.metadata
            )
        }
    }
}

data class CreateActivityRequest(
    val category: ActivityCategory,
    val message: String,
    @JsonProperty("user_id")
    val userId: String? = null,
    @JsonProperty("user_name")
    val userName: String? = null,
    val status: ActivityStatus? = null,
    @JsonProperty("status_text")
    val statusText: String? = null,
    @JsonProperty("primary_entity")
    val primaryEntity: RelatedEntityDto? = null,
    @JsonProperty("related_entities")
    val relatedEntities: List<RelatedEntityDto> = emptyList(),
    val metadata: Map<String, Any>? = null
)