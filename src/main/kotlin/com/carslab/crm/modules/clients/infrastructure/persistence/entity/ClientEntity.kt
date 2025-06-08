package com.carslab.crm.modules.clients.infrastructure.persistence.entity

import com.carslab.crm.modules.clients.domain.model.Client
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.CreateClient
import com.carslab.crm.modules.clients.domain.model.shared.AuditInfo
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime

@Entity
@Table(
    name = "clients",
    indexes = [
        Index(name = "idx_client_email", columnList = "email"),
        Index(name = "idx_client_phone", columnList = "phone"),
        Index(name = "idx_client_company_id", columnList = "company_id"),
        Index(name = "idx_client_full_name", columnList = "first_name, last_name")
    ]
)
class ClientEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Column(name = "email", nullable = false, length = 255)
    var email: String,

    @Column(name = "phone", nullable = false, length = 50)
    var phone: String,

    @Column(name = "address", length = 500)
    var address: String? = null,

    @Column(name = "company", length = 200)
    var company: String? = null,

    @Column(name = "tax_id", length = 50)
    var taxId: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) {

    // Lazy-loaded associations with proper fetch strategies
    @OneToMany(
        mappedBy = "client",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.REMOVE]
    )
    @BatchSize(size = 50)
    var associations: MutableSet<ClientVehicleAssociationEntity> = mutableSetOf()

    @OneToOne(
        mappedBy = "client",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var statistics: ClientStatisticsEntity? = null

    fun toDomain(): Client = Client(
        id = ClientId.of(id!!),
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        address = address,
        company = company,
        taxId = taxId,
        notes = notes,
        audit = AuditInfo(
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version
        )
    )

    companion object {
        fun fromDomain(client: CreateClient, companyId: Long): ClientEntity = ClientEntity(
            id = null,
            companyId = companyId,
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt,
            createdBy = client.audit.createdBy,
            updatedBy = client.audit.updatedBy,
            version = client.audit.version
        )

        fun fromDomain(client: Client, companyId: Long): ClientEntity = ClientEntity(
            id = null,
            companyId = companyId,
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt,
            createdBy = client.audit.createdBy,
            updatedBy = client.audit.updatedBy,
            version = client.audit.version
        )
    }
}