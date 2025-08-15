// src/main/kotlin/com/carslab/crm/production/modules/activities/domain/model/Activity.kt
package com.carslab.crm.production.modules.activities.domain.model

import java.time.LocalDateTime
import java.util.*

@JvmInline
value class ActivityId(val value: String) {
    companion object {
        fun generate(): ActivityId = ActivityId(UUID.randomUUID().toString())
        fun of(value: String): ActivityId = ActivityId(value)
    }
}

enum class ActivityCategory {
    APPOINTMENT,
    PROTOCOL,
    COMMENT,
    CLIENT,
    VEHICLE,
    NOTIFICATION,
    SYSTEM,
    DOCUMENT
}

enum class ActivityStatus {
    SUCCESS,
    PENDING,
    ERROR
}

data class Activity(
    val id: ActivityId,
    val companyId: Long,
    val timestamp: LocalDateTime,
    val category: ActivityCategory,
    val message: String,
    val userId: String? = null,
    val userName: String? = null,
    val status: ActivityStatus? = null,
    val statusText: String? = null,
    val primaryEntityId: String? = null,
    val primaryEntityType: String? = null,
    val primaryEntityName: String? = null,
    val metadata: Map<String, Any>? = null,
    val createdAt: LocalDateTime
) {
    fun belongsToCompany(companyId: Long): Boolean {
        return this.companyId == companyId
    }
}