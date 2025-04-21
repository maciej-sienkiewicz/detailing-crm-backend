package com.carslab.crm.domain

import com.carslab.crm.domain.model.ConfigureRolePermissionCommand
import com.carslab.crm.domain.model.PermissionCheck
import com.carslab.crm.domain.model.PermissionDto
import com.carslab.crm.domain.model.UpdateDataFieldPermissionsCommand
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.persistence.entity.DataField
import com.carslab.crm.infrastructure.persistence.entity.PermissionConfiguration
import com.carslab.crm.infrastructure.persistence.entity.PermissionConfigurationEntity
import com.carslab.crm.infrastructure.persistence.entity.ResourceType
import com.carslab.crm.infrastructure.persistence.repository.PermissionConfigurationRepository
import com.carslab.crm.infrastructure.persistence.repository.PermissionRepository
import com.carslab.crm.infrastructure.persistence.repository.RoleRepository
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PermissionService(
    private val permissionRepository: PermissionRepository,
    private val permissionConfigRepository: PermissionConfigurationRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {
    fun hasPermission(userId: Long, permission: PermissionCheck): Boolean {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        // Get all roles for user
        val userRoles = user.roles

        // Check if any role has the required permission
        return userRoles.any { role ->
            // Get permission configurations for this role
            val permissionConfigs = permissionConfigRepository
                .findByRoleIdAndEnabled(role.id!!, true)

            // Get the specific permission entity
            val permissionEntity = permissionRepository
                .findByResourceTypeAndAction(permission.resourceType, permission.action)
                .orElse(null) ?: return@any false

            // Check if permission is configured for this role
            permissionConfigs.any { config ->
                config.permissionId == permissionEntity.id
                        && validateConstraints(config, permission.resourceId)
            }
        }
    }

    private fun validateConstraints(config: PermissionConfigurationEntity, resourceId: String?): Boolean {
        if (config.constraints.isNullOrBlank() || resourceId == null) {
            return true
        }


        return true
    }

    private fun checkOwnership(userId: Long, resourceId: String): Boolean {
        // Implement logic to check if resource is owned by user
        // This is just a placeholder - real implementation would depend on your domain
        return true
    }

    fun getUserPermissions(userId: Long): List<PermissionDto> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val userRoles = user.roles

        // Collect all enabled permissions from all roles
        val enabledPermissions = mutableSetOf<PermissionDto>()

        userRoles.forEach { role ->
            val permissionConfigs = permissionConfigRepository
                .findByRoleIdAndEnabled(role.id!!, true)

            permissionConfigs.forEach { config ->
                val permission = permissionRepository.findById(config.permissionId)
                    .orElse(null) ?: return@forEach

                enabledPermissions.add(
                    PermissionDto(
                        id = permission.id!!,
                        name = permission.name,
                        resourceType = permission.resourceType,
                        action = permission.action,
                        constraints = config.constraints
                    )
                )
            }
        }

        return enabledPermissions.toList()
    }

    fun getUserRoles(userId: Long): List<String> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        return user.roles.map { it.name }
    }
}