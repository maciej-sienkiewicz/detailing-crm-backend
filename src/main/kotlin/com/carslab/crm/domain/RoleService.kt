package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.infrastructure.exception.BusinessException
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.persistence.entity.PermissionConfiguration
import com.carslab.crm.infrastructure.persistence.entity.PermissionConfigurationEntity
import com.carslab.crm.infrastructure.persistence.entity.Role
import com.carslab.crm.infrastructure.persistence.entity.RoleEntity
import com.carslab.crm.infrastructure.persistence.repository.PermissionConfigurationRepository
import com.carslab.crm.infrastructure.persistence.repository.PermissionRepository
import com.carslab.crm.infrastructure.persistence.repository.RoleRepository
import com.carslab.crm.infrastructure.persistence.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val permissionConfigRepository: PermissionConfigurationRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createRole(createRoleCommand: CreateRoleCommand): RoleResponse {
        // Sprawdź, czy nazwa roli już istnieje dla tej firmy
        if (roleRepository.findByNameAndCompanyId(createRoleCommand.name, createRoleCommand.companyId) != null) {
            throw BusinessException("Role with name '${createRoleCommand.name}' already exists for this company")
        }

        // Utwórz obiekt domeny
        val role = Role(
            name = createRoleCommand.name,
            description = createRoleCommand.description,
            companyId = createRoleCommand.companyId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // Konwertuj do encji i zapisz
        val roleEntity = RoleEntity.fromDomain(role)
        val savedRoleEntity = roleRepository.save(roleEntity)

        // Przypisz uprawnienia
        if (createRoleCommand.useDefaultPermissions) {
            setDefaultPermissions(savedRoleEntity.id!!, createRoleCommand.companyId)
        } else if (createRoleCommand.initialPermissionIds.isNotEmpty()) {
            assignPermissionsToRole(savedRoleEntity.id!!, createRoleCommand.initialPermissionIds)
        }

        return RoleResponse.fromEntity(savedRoleEntity)
    }

    @Transactional
    fun updateRole(updateRoleCommand: UpdateRoleCommand): RoleResponse {
        val roleEntity = roleRepository.findById(updateRoleCommand.id)
            .orElseThrow { ResourceNotFoundException("Role not found with id: ${updateRoleCommand.id}") }

        // Sprawdź, czy rola należy do tej samej firmy
        if (roleEntity.companyId != updateRoleCommand.companyId) {
            throw SecurityException("Cannot update role from a different company")
        }

        // Sprawdź, czy nazwa nie koliduje z inną rolą
        if (roleRepository.existsByNameForCompanyExcludingId(
                updateRoleCommand.name,
                updateRoleCommand.companyId,
                updateRoleCommand.id
            )
        ) {
            throw BusinessException("Role with name '${updateRoleCommand.name}' already exists for this company")
        }

        // Konwertuj do domeny, zaktualizuj i przekonwertuj z powrotem do encji
        val role = roleEntity.toDomain().copy(
            name = updateRoleCommand.name,
            description = updateRoleCommand.description,
            updatedAt = LocalDateTime.now()
        )

        roleEntity.updateFromDomain(role)
        val updatedRoleEntity = roleRepository.save(roleEntity)

        return RoleResponse.fromEntity(updatedRoleEntity)
    }

    @Transactional
    fun deleteRole(roleId: Long, companyId: Long) {
        val roleEntity = roleRepository.findById(roleId)
            .orElseThrow { ResourceNotFoundException("Role not found with id: $roleId") }

        // Sprawdź, czy rola należy do tej samej firmy
        if (roleEntity.companyId != companyId) {
            throw SecurityException("Cannot delete role from a different company")
        }

        // Sprawdź, czy rola jest używana przez użytkowników
        val usersWithRole = userRepository.findByRoleId(roleId)
        if (usersWithRole.isNotEmpty()) {
            throw BusinessException("Cannot delete role that is assigned to ${usersWithRole.size} users")
        }

        // Usuń konfiguracje uprawnień dla tej roli
        permissionConfigRepository.deleteAllByRoleId(roleId)

        // Usuń rolę
        roleRepository.delete(roleEntity)
    }

    @Transactional
    fun getRoleById(roleId: Long, companyId: Long): RoleDetailResponse {
        val roleEntity = roleRepository.findById(roleId)
            .orElseThrow { ResourceNotFoundException("Role not found with id: $roleId") }

        // Sprawdź, czy rola należy do tej samej firmy
        if (roleEntity.companyId != companyId) {
            throw SecurityException("Cannot access role from a different company")
        }

        // Pobierz konfiguracje uprawnień dla tej roli
        val permissionConfigs = permissionConfigRepository.findByRoleId(roleId)

        // Mapuj uprawnienia na DTO
        val permissions = permissionRepository.findAllOrdered().map { permissionEntity ->
            val config = permissionConfigs.find { it.permissionId == permissionEntity.id }
            PermissionSummaryDto(
                id = permissionEntity.id!!,
                name = permissionEntity.name,
                resourceType = permissionEntity.resourceType,
                action = permissionEntity.action,
                isEnabled = config?.enabled ?: false
            )
        }

        // Policz użytkowników z tą rolą
        val userCount = userRepository.findByRoleId(roleId).size

        val role = roleEntity.toDomain()

        return RoleDetailResponse(
            id = role.id!!,
            name = role.name,
            description = role.description,
            companyId = role.companyId,
            permissions = permissions,
            userCount = userCount,
            createdAt = role.createdAt,
            updatedAt = role.updatedAt
        )
    }

    @Transactional
    fun getRolePermissions(roleId: Long): List<PermissionConfigDto> {
        val roleEntity = roleRepository.findById(roleId)
            .orElseThrow { ResourceNotFoundException("Role not found with id: $roleId") }

        // Pobierz konfiguracje uprawnień dla tej roli
        val permissionConfigs = permissionConfigRepository.findByRoleId(roleId)

        // Mapuj uprawnienia na DTO
        return permissionConfigs.mapNotNull { configEntity ->
            val permissionEntity = permissionRepository.findById(configEntity.permissionId).orElse(null)
                ?: return@mapNotNull null

            val config = configEntity.toDomain()
            val permission = permissionEntity.toDomain()

            PermissionConfigDto(
                id = configEntity.id!!,
                permissionId = permission.id!!,
                permissionName = permission.name,
                resourceType = permission.resourceType,
                action = permission.action,
                enabled = config.enabled,
                constraints = config.constraints
            )
        }
    }

    @Transactional
    fun configureRolePermission(configCommand: ConfigureRolePermissionCommand) {
        // Validate that the role belongs to the company
        val roleEntity = roleRepository.findById(configCommand.roleId)
            .orElseThrow { ResourceNotFoundException("Role not found with id: ${configCommand.roleId}") }

        if (roleEntity.companyId != configCommand.companyId) {
            throw SecurityException("Cannot configure role from a different company")
        }

        // Validate that the permission exists
        val permissionEntity = permissionRepository.findById(configCommand.permissionId)
            .orElseThrow { ResourceNotFoundException("Permission not found with id: ${configCommand.permissionId}") }

        // Find existing config or create new one
        val existingConfigEntity = permissionConfigRepository
            .findByRoleIdAndPermissionId(configCommand.roleId, configCommand.permissionId)

        if (existingConfigEntity != null) {
            val existingConfig = existingConfigEntity.toDomain()
            val updatedConfig = existingConfig.copy(
                enabled = configCommand.enabled,
                constraints = configCommand.constraints,
                updatedAt = LocalDateTime.now()
            )

            existingConfigEntity.updateFromDomain(updatedConfig)
            permissionConfigRepository.save(existingConfigEntity)
        } else {
            val newConfig = PermissionConfiguration(
                roleId = configCommand.roleId,
                permissionId = configCommand.permissionId,
                companyId = configCommand.companyId,
                enabled = configCommand.enabled,
                constraints = configCommand.constraints,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                createdBy = configCommand.userId
            )

            val configEntity = PermissionConfigurationEntity.fromDomain(newConfig)
            permissionConfigRepository.save(configEntity)
        }
    }

    private fun setDefaultPermissions(roleId: Long, companyId: Long) {
        // Pobierz domyślne uprawnienia dla firmy
        val defaultPermissions = permissionRepository.findDefaultPermissionsForCompany(companyId)

        // Przypisz uprawnienia do roli
        defaultPermissions.forEach { permissionEntity ->
            val config = PermissionConfiguration(
                roleId = roleId,
                permissionId = permissionEntity.id!!,
                companyId = companyId,
                enabled = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                constraints = null,
                createdBy = null
            )

            val configEntity = PermissionConfigurationEntity.fromDomain(config)
            permissionConfigRepository.save(configEntity)
        }
    }

    private fun assignPermissionsToRole(roleId: Long, permissionIds: List<Long>) {
        permissionIds.forEach { permissionId ->
            // Sprawdź, czy uprawnienie istnieje
            if (permissionRepository.existsById(permissionId)) {
                val companyId = roleRepository.findById(roleId).get().companyId

                val config = PermissionConfiguration(
                    roleId = roleId,
                    permissionId = permissionId,
                    companyId = companyId,
                    enabled = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    constraints = null,
                    createdBy = null
                )

                val configEntity = PermissionConfigurationEntity.fromDomain(config)
                permissionConfigRepository.save(configEntity)
            }
        }
    }
}