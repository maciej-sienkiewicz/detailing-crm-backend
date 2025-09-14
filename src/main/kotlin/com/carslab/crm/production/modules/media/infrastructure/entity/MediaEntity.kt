package com.carslab.crm.production.modules.media.infrastructure.entity

import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "media",
    indexes = [
        Index(name = "idx_media_company_context", columnList = "companyId,context,entityId"),
        Index(name = "idx_media_visit", columnList = "visitId"),
        Index(name = "idx_media_vehicle", columnList = "vehicleId"),
        Index(name = "idx_media_company_created", columnList = "companyId,createdAt"),
        Index(name = "idx_media_company_context_created", columnList = "companyId,context,createdAt")
    ]
)
class MediaEntity(
    @Id
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val context: MediaContext,

    @Column(name = "entity_id")
    val entityId: Long? = null,

    @Column(name = "visit_id")
    val visitId: Long? = null,

    @Column(name = "vehicle_id")
    val vehicleId: Long? = null,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(length = 100)
    val location: String? = null,

    @Column(columnDefinition = "TEXT")
    val tags: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    val type: MediaType,

    @Column(nullable = false)
    val size: Long,

    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime
)