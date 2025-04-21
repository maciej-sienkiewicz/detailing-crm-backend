package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false)
    var email: String,

    var firstName: String,
    var lastName: String,

    @Column(name = "company_id")
    var companyId: Long,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<RoleEntity> = mutableSetOf(),

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): User = User(
        id = this.id,
        username = this.username,
        passwordHash = this.passwordHash,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        companyId = this.companyId,
        roles = this.roles.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun updateFromDomain(domain: User) {
        this.username = domain.username
        this.email = domain.email
        this.firstName = domain.firstName
        this.lastName = domain.lastName
        this.updatedAt = LocalDateTime.now()
        // Uwaga: nie aktualizujemy hasła ani ról bezpośrednio, to powinno być robione przez osobne metody
    }

    fun updatePassword(passwordHash: String) {
        this.passwordHash = passwordHash
        this.updatedAt = LocalDateTime.now()
    }

    fun addRole(role: RoleEntity) {
        this.roles.add(role)
        this.updatedAt = LocalDateTime.now()
    }

    fun removeRole(roleId: Long) {
        this.roles.removeIf { it.id == roleId }
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun fromDomain(domain: User): UserEntity {
            val entity = UserEntity(
                id = domain.id,
                username = domain.username,
                passwordHash = domain.passwordHash,
                email = domain.email,
                firstName = domain.firstName,
                lastName = domain.lastName,
                companyId = domain.companyId,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )

            // Uwaga: nie konwertujemy ról, to wymaga osobnej obsługi

            return entity
        }
    }
}

// Model domeny dla użytkownika
data class User(
    val id: Long? = null,
    val username: String,
    val passwordHash: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyId: Long,
    val roles: Set<Role> = emptySet(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)