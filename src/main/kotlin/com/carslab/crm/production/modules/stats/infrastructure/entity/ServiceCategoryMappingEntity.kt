package com.carslab.crm.production.modules.stats.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "service_category_mappings",
    indexes = [
        Index(name = "idx_mappings_service_id", columnList = "service_id"),
        Index(name = "idx_mappings_category_id", columnList = "category_id"),
        Index(name = "idx_mappings_unique", columnList = "service_id,category_id", unique = true)
    ]
)
class ServiceCategoryMappingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "service_id", nullable = false, length = 36)
    val serviceId: String,

    @Column(name = "category_id", nullable = false)
    val categoryId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
