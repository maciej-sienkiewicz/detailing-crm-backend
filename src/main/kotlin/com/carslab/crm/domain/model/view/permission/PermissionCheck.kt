package com.carslab.crm.domain.model.view.permission

import com.carslab.crm.infrastructure.persistence.entity.PermissionAction
import com.carslab.crm.infrastructure.persistence.entity.ResourceType

data class PermissionCheck(
    val resourceType: ResourceType,
    val action: PermissionAction,
    val resourceId: String? = null
)
