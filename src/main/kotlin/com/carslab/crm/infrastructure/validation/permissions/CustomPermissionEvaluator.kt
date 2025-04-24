package com.carslab.crm.infrastructure.validation.permissions

import com.carslab.crm.domain.PermissionService
import com.carslab.crm.domain.model.view.permission.PermissionCheck
import com.carslab.crm.infrastructure.persistence.entity.PermissionAction
import com.carslab.crm.infrastructure.persistence.entity.ResourceType
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class CustomPermissionEvaluator(
    private val permissionService: PermissionService
) : PermissionEvaluator {

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any
    ): Boolean {
        val userId = authentication.principal as Long

        val resourceType = ResourceType.valueOf(targetType)
        val action = PermissionAction.valueOf(permission.toString())

        return permissionService.hasPermission(
            userId,
            PermissionCheck(
                resourceType = resourceType,
                action = action,
                resourceId = targetId.toString()
            )
        )
    }

    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any,
        permission: Any
    ): Boolean {
        // Implementation for object-level permissions if needed
        return false
    }
}