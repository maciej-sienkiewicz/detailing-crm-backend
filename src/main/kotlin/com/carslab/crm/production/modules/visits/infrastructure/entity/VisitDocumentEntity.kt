package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.visits.domain.model.*
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "visit_documents",
    indexes = [
        Index(name = "idx_visit_documents_visit_id", columnList = "visitId"),
        Index(name = "idx_visit_documents_type", columnList = "type")
    ]
)
class VisitDocumentEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    val visitId: Long,

    @Column(nullable = false, length = 255)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: DocumentType,

    @Column(nullable = false)
    val size: Long,

    @Column(nullable = false, length = 100)
    val contentType: String,

    @Column(length = 500)
    val description: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = false, length = 100)
    val uploadedBy: String
) {
    fun toDomain(): VisitDocument {
        return VisitDocument(
            id = id,
            visitId = VisitId.of(visitId),
            name = name,
            type = type,
            size = size,
            contentType = contentType,
            description = description,
            createdAt = createdAt,
            uploadedBy = uploadedBy
        )
    }

    companion object {
        fun fromDomain(document: VisitDocument, visitId: Long): VisitDocumentEntity {
            return VisitDocumentEntity(
                id = document.id,
                visitId = visitId,
                name = document.name,
                type = document.type,
                size = document.size,
                contentType = document.contentType,
                description = document.description,
                createdAt = document.createdAt,
                uploadedBy = document.uploadedBy
            )
        }
    }
}