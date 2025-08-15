package com.carslab.crm.production.modules.services.infrastructure.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "services",
    indexes = [
        Index(name = "idx_services_company_id", columnList = "company_id"),
        Index(name = "idx_services_active", columnList = "company_id,is_active"),
        Index(name = "idx_services_name", columnList = "company_id,name,is_active")
    ]
)
class ServiceEntity(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 500)
    val description: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Column(name = "vat_rate", nullable = false)
    val vatRate: Int,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "previous_version_id", length = 36)
    val previousVersionId: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0
)