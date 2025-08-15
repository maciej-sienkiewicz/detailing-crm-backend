package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "companiess")
class CompanyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    var address: String? = null,
    var taxId: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): Company = Company(
        id = this.id,
        name = this.name,
        address = this.address,
        taxId = this.taxId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    fun updateFromDomain(domain: Company) {
        this.name = domain.name
        this.address = domain.address
        this.taxId = domain.taxId
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun fromDomain(domain: Company): CompanyEntity = CompanyEntity(
            id = domain.id,
            name = domain.name,
            address = domain.address,
            taxId = domain.taxId,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

// Model domeny dla firmy
data class Company(
    val id: Long? = null,
    val name: String,
    val address: String? = null,
    val taxId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)