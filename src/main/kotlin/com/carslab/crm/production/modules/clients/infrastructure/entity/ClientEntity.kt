package com.carslab.crm.production.modules.clients.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "clients",
    indexes = [
        Index(name = "idx_client_company_id", columnList = "company_id"),
        Index(name = "idx_client_email", columnList = "company_id,email"),
        Index(name = "idx_client_phone", columnList = "company_id,phone"),
        Index(name = "idx_client_full_name", columnList = "company_id,first_name,last_name"),
        Index(name = "idx_client_active", columnList = "company_id,active")
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

    @Column(name = "active", nullable = false)
    var active: Boolean = true
)