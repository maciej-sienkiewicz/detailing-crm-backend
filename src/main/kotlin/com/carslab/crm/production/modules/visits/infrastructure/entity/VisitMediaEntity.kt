package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "visit_media",
    indexes = [
        Index(name = "idx_visit_media_visit_id", columnList = "visitId"),
        Index(name = "idx_visit_media_type", columnList = "type"),
        Index(name = "idx_visit_media_company_visit", columnList = "visitId, tags"),
        Index(name = "idx_visit_media_created_at", columnList = "createdAt")
    ]
)
class VisitMediaEntity(
    @Id
    val id: String,

    @Column(name = "visit_id", nullable = false)
    val visitId: Long,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(length = 1000)
    val description: String? = null,

    @Column(length = 100)
    val location: String? = null,

    @Column(length = 2000)
    val tags: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: MediaType,

    @Column(nullable = false)
    val size: Long,

    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime
) {
    fun toDomain(): VisitMedia {
        return VisitMedia(
            id = id,
            visitId = visitId,
            name = name,
            description = description,
            location = location,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            type = type,
            size = size,
            contentType = contentType,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(media: VisitMedia): VisitMediaEntity {
            return VisitMediaEntity(
                id = media.id,
                visitId = media.visitId,
                name = media.name,
                description = media.description,
                location = media.location,
                tags = media.tags.joinToString(","),
                type = media.type,
                size = media.size,
                contentType = media.contentType,
                createdAt = media.createdAt,
                updatedAt = media.updatedAt
            )
        }
    }
}