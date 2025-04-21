package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "roles")
class RoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var description: String,

    @Column(name = "company_id")
    var companyId: Long,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    var permissions: MutableSet<PermissionEntity> = mutableSetOf(),

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): Role = Role(
        id = this.id,
        name = this.name,
        description = this.description,
        companyId = this.companyId,
        permissions = this.permissions.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun updateFromDomain(domain: Role) {
        this.name = domain.name
        this.description = domain.description
        this.updatedAt = LocalDateTime.now()
        // Nie aktualizujemy bezpośrednio permissions, ponieważ to wymaga osobnej logiki
    }

    companion object {
        fun fromDomain(domain: Role): RoleEntity {
            val entity = RoleEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                companyId = domain.companyId,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )

            // W rzeczywistej implementacji musielibyśmy załadować rzeczywiste encje PermissionEntity
            // zamiast próbować je konwertować z domeny

            return entity
        }
    }
}

// Model domeny dla roli
data class Role(
    val id: Long? = null,
    val name: String,
    val description: String,
    val companyId: Long,
    val permissions: Set<Permission> = emptySet(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)