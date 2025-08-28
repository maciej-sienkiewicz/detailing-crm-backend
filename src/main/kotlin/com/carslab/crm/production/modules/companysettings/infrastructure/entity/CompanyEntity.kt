package com.carslab.crm.production.modules.companysettings.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "companies",
    indexes = [
        Index(name = "idx_companies_tax_id", columnList = "tax_id", unique = true)
    ]
)
class CompanyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false, length = 200)
    var name: String,

    @Column(name = "tax_id", nullable = false, length = 20, unique = true)
    var taxId: String,

    @Column(name = "address", length = 500)
    var address: String? = null,

    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Column(name = "website", length = 255)
    var website: String? = null,

    @Column(name = "logo_id", length = 255)
    var logoId: String? = null,

    @Column(name = "bank_account_number", length = 50)
    var bankAccountNumber: String? = null,

    @Column(name = "bank_name", length = 100)
    var bankName: String? = null,

    @Column(name = "swift_code", length = 20)
    var swiftCode: String? = null,

    @Column(name = "account_holder_name", length = 200)
    var accountHolderName: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
)