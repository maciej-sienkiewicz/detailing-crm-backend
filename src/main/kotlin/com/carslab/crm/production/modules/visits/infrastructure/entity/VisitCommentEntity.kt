package com.carslab.crm.production.modules.visits.infrastructure.entity

import com.carslab.crm.production.modules.visits.domain.model.*
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import com.carslab.crm.production.modules.visits.domain.models.enums.CommentType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "visit_comments",
    indexes = [
        Index(name = "idx_visit_comments_visit_id", columnList = "visitId"),
        Index(name = "idx_visit_comments_created_at", columnList = "createdAt")
    ]
)
class VisitCommentEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    val visitId: Long,

    @Column(nullable = false, length = 100)
    val author: String,

    @Column(nullable = false, length = 2000)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: CommentType,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column
    val updatedAt: LocalDateTime? = null
) {
    fun toDomain(): VisitComment {
        return VisitComment(
            id = id,
            visitId = VisitId.of(visitId),
            author = author,
            content = content,
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(comment: VisitComment, visitId: Long): VisitCommentEntity {
            return VisitCommentEntity(
                id = comment.id ?: UUID.randomUUID().toString(),
                visitId = visitId,
                author = comment.author,
                content = comment.content,
                type = comment.type,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
        }
    }
}