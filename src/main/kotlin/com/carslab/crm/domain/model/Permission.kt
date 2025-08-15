package com.carslab.crm.domain.model

import com.carslab.crm.infrastructure.persistence.entity.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class PermissionDto(
    val id: Long,
    val name: String,
    val resourceType: ResourceType,
    val action: PermissionAction,
    val constraints: String? = null
)

data class PermissionConfigDto(
    val id: Long,
    val permissionId: Long,
    val permissionName: String,
    val resourceType: ResourceType,
    val action: PermissionAction,
    val enabled: Boolean,
    val constraints: String? = null
)

data class ConfigureRolePermissionCommand(
    val roleId: Long = 0,
    val permissionId: Long,
    val companyId: Long,
    val enabled: Boolean,
    val constraints: String? = null,
    val userId: Long
)

data class CreateRoleCommand(
    @JsonProperty("name")
    val name: String = "",

    @JsonProperty("description")
    val description: String = "",

    @JsonProperty("companyId")
    val companyId: Long = 0,

    @JsonProperty("useDefaultPermissions")
    val useDefaultPermissions: Boolean = true,

    @JsonProperty("initialPermissionIds")
    val initialPermissionIds: List<Long> = emptyList()
)

data class UpdateRoleCommand(
    val id: Long,
    val name: String,
    val description: String,
    val companyId: Long
)

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String,
    val companyId: Long,
    val permissionCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: RoleEntity): RoleResponse {
            return RoleResponse(
                id = entity.id ?: throw IllegalStateException("Role ID cannot be null"),
                name = entity.name,
                description = entity.description,
                companyId = entity.companyId,
                permissionCount = entity.permissions.size,
                createdAt = entity.createdAt ?: LocalDateTime.now(),
                updatedAt = entity.updatedAt ?: LocalDateTime.now()
            )
        }
    }
}

data class RoleDetailResponse(
    val id: Long,
    val name: String,
    val description: String,
    val companyId: Long,
    val permissions: List<PermissionSummaryDto>,
    val userCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class PermissionSummaryDto(
    val id: Long,
    val name: String,
    val resourceType: ResourceType,
    val action: PermissionAction,
    val isEnabled: Boolean
)

data class RoleWithPermissionsDto(
    val id: Long,
    val name: String,
    val description: String,
    val permissions: List<PermissionConfigDto>
)

data class CreateUserCommand(
    @JsonProperty("username")
    val username: String = "",
    @JsonProperty("password")
    val password: String = "",
    @JsonProperty("email")
    val email: String = "",
    @JsonProperty("firstName")
    val firstName: String = "",
    @JsonProperty("lastName")
    val lastName: String = "",
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyId: Long,
    val roles: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: UserEntity): UserResponse {
            return UserResponse(
                id = entity.id ?: throw IllegalStateException("User ID cannot be null"),
                username = entity.username,
                email = entity.email,
                firstName = entity.firstName,
                lastName = entity.lastName,
                companyId = entity.companyId,
                roles = entity.roles.map { it.name },
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class UpdateDataFieldPermissionsCommand(
    val roleId: Long,
    val companyId: Long,
    val userId: Long,
    val fieldConfigs: List<FieldConfigUpdate>
)

data class FieldConfigUpdate(
    val fieldId: String,
    val isEnabled: Boolean
)

data class ChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String
)

data class UpdateProfileCommand(
    val email: String,
    val firstName: String,
    val lastName: String
)

data class CreateCompanyCommand(
    val name: String,
    val address: String? = null,
    val taxId: String? = null,
    val adminUser: CreateUserCommand? = null
)

data class UpdateCompanyCommand(
    val id: Long,
    val name: String,
    val address: String? = null,
    val taxId: String? = null
)

data class CompanyResponse(
    val id: Long,
    val name: String,
    val address: String?,
    val taxId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: CompanyEntities): CompanyResponse {
            return CompanyResponse(
                id = entity.id ?: throw IllegalStateException("Company ID cannot be null"),
                name = entity.name,
                address = entity.address,
                taxId = entity.taxId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}