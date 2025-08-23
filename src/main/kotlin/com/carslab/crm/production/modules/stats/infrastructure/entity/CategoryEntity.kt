package com.carslab.crm.production.modules.stats.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "service_categories",
    indexes = [
        Index(name = "idx_categories_company_id", columnList = "company_id"),
        Index(name = "idx_categories_name", columnList = "company_id,name")
    ]
)
class CategoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
