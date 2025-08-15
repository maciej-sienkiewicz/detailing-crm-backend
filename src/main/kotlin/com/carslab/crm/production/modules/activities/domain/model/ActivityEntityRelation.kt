// src/main/kotlin/com/carslab/crm/production/modules/activities/domain/model/ActivityEntityRelation.kt
package com.carslab.crm.production.modules.activities.domain.model

import java.util.*

@JvmInline
value class ActivityEntityRelationId(val value: String) {
    companion object {
        fun generate(): ActivityEntityRelationId = ActivityEntityRelationId(UUID.randomUUID().toString())
        fun of(value: String): ActivityEntityRelationId = ActivityEntityRelationId(value)
    }
}

data class ActivityEntityRelation(
    val id: ActivityEntityRelationId,
    val activityId: ActivityId,
    val entityId: String,
    val entityType: String,
    val entityName: String
)