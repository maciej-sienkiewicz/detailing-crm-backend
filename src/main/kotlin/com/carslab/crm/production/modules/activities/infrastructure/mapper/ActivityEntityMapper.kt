// src/main/kotlin/com/carslab/crm/production/modules/activities/infrastructure/mapper/ActivityEntityMapper.kt
package com.carslab.crm.production.modules.activities.infrastructure.mapper

import com.carslab.crm.production.modules.activities.domain.model.Activity
import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelation
import com.carslab.crm.production.modules.activities.domain.model.ActivityEntityRelationId
import com.carslab.crm.production.modules.activities.domain.model.ActivityId
import com.carslab.crm.production.modules.activities.infrastructure.entity.ActivityEntity
import com.carslab.crm.production.modules.activities.infrastructure.entity.ActivityEntityRelationEntity
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private val objectMapper = jacksonObjectMapper()

fun Activity.toEntity(): ActivityEntity {
    return ActivityEntity(
        id = this.id.value,
        companyId = this.companyId,
        timestamp = this.timestamp,
        category = this.category,
        message = this.message,
        description = this.description,
        userId = this.userId,
        userName = this.userName,
        status = this.status,
        statusText = this.statusText,
        primaryEntityId = this.primaryEntityId,
        primaryEntityType = this.primaryEntityType,
        primaryEntityName = this.primaryEntityName,
        metadataJson = serializeMetadata(this.metadata),
        createdAt = this.createdAt
    )
}

fun ActivityEntity.toDomain(): Activity {
    return Activity(
        id = ActivityId.of(this.id),
        companyId = this.companyId,
        timestamp = this.timestamp,
        category = this.category,
        message = this.message,
        description = this.description,
        userId = this.userId,
        userName = this.userName,
        status = this.status,
        statusText = this.statusText,
        primaryEntityId = this.primaryEntityId,
        primaryEntityType = this.primaryEntityType,
        primaryEntityName = this.primaryEntityName,
        metadata = deserializeMetadata(this.metadataJson),
        createdAt = this.createdAt
    )
}

fun ActivityEntityRelation.toEntity(): ActivityEntityRelationEntity {
    return ActivityEntityRelationEntity(
        id = this.id.value,
        activityId = this.activityId.value,
        entityId = this.entityId,
        entityType = this.entityType,
        entityName = this.entityName
    )
}

fun ActivityEntityRelationEntity.toDomain(): ActivityEntityRelation {
    return ActivityEntityRelation(
        id = ActivityEntityRelationId.of(this.id),
        activityId = ActivityId.of(this.activityId),
        entityId = this.entityId,
        entityType = this.entityType,
        entityName = this.entityName
    )
}

private fun serializeMetadata(metadata: Map<String, Any>?): String? {
    return metadata?.let {
        try {
            objectMapper.writeValueAsString(it)
        } catch (e: Exception) {
            null
        }
    }
}

private fun deserializeMetadata(json: String?): Map<String, Any>? {
    return json?.let {
        try {
            objectMapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
        } catch (e: Exception) {
            null
        }
    }
}