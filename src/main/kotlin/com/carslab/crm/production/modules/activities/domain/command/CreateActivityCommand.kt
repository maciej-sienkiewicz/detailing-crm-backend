package com.carslab.crm.production.modules.activities.domain.command

import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.activities.domain.model.ActivityStatus

data class RelatedEntity(
    val id: String,
    val type: String,
    val name: String
)

data class CreateActivityCommand(
    val companyId: Long,
    val category: ActivityCategory,
    val message: String,
    val userId: String? = null,
    val userName: String? = null,
    val status: ActivityStatus? = null,
    val statusText: String? = null,
    val primaryEntity: RelatedEntity? = null,
    val relatedEntities: List<RelatedEntity> = emptyList(),
    val metadata: Map<String, Any>? = null
)