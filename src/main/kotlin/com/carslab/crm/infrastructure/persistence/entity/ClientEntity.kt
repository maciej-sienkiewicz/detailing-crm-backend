package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.create.client.CreateClientModel
import jakarta.persistence.*
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@Entity
@Table(name = "clients")
class ClientEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var companyId: Long,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = true)
    var phone: String? = null,

    @Column(nullable = true)
    var address: String? = null,

    @Column(nullable = true)
    var company: String? = null,

    @Column(name = "tax_id", nullable = true)
    var taxId: String? = null,

    @Column(nullable = true)
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    // Relacja many-to-many używająca join table
    @ManyToMany(mappedBy = "owners")
    var vehicles: MutableSet<VehicleEntity> = mutableSetOf()

) {
    fun toDomain(): ClientDetails = ClientDetails(
        id = ClientId(id!!),
        firstName = firstName,
        lastName = lastName,
        email = email ?: "",
        phone = phone ?: "",
        address = address,
        company = company,
        taxId = taxId,
        notes = notes,
        audit = com.carslab.crm.domain.model.ClientAudit(
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    )

    companion object {
        fun fromDomain(domain: ClientDetails): ClientEntity {
            return ClientEntity(
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                firstName = domain.firstName,
                lastName = domain.lastName,
                email = domain.email,
                phone = domain.phone,
                address = domain.address,
                company = domain.company,
                taxId = domain.taxId,
                notes = domain.notes,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }

        fun fromDomain(domain: CreateClientModel): ClientEntity {
            return ClientEntity(
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                firstName = domain.firstName,
                lastName = domain.lastName,
                email = domain.email,
                phone = domain.phone,
                address = domain.address,
                company = domain.company,
                taxId = domain.taxId,
                notes = domain.notes,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}