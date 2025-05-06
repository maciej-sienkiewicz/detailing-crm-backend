package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "permission_configurations")
class PermissionConfigurationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, name = "company_id")
    var companyId: Long,

    @Column(nullable = false, name = "role_id")
    var roleId: Long,

    @Column(nullable = false, name = "permission_id")
    var permissionId: Long,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "constraints")
    var constraints: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: Long? = null
) {
    fun toDomain(): PermissionConfiguration = PermissionConfiguration(
        id = this.id,
        companyId = this.companyId,
        roleId = this.roleId,
        permissionId = this.permissionId,
        enabled = this.enabled,
        constraints = this.constraints,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        createdBy = this.createdBy
    )

    fun updateFromDomain(domain: PermissionConfiguration) {
        this.enabled = domain.enabled
        this.constraints = domain.constraints
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun fromDomain(domain: PermissionConfiguration): PermissionConfigurationEntity =
            PermissionConfigurationEntity(
                id = domain.id,
                companyId = domain.companyId,
                roleId = domain.roleId,
                permissionId = domain.permissionId,
                enabled = domain.enabled,
                constraints = domain.constraints,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                createdBy = domain.createdBy
            )
    }
}

// Model domeny dla konfiguracji uprawnień
data class PermissionConfiguration(
    val id: Long? = null,
    val companyId: Long,
    val roleId: Long,
    val permissionId: Long,
    val enabled: Boolean,
    val constraints: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: Long? = null
)