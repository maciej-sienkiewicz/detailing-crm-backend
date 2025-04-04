package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "protocol_comments")
class ProtocolCommentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    var protocol: ProtocolEntity,

    @Column(nullable = false)
    var author: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    var timestamp: String,

    @Column(nullable = false)
    var type: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ProtocolComment {
        return ProtocolComment(
            id = id,
            protocolId = ProtocolId(protocol.id.toString()),
            author = author,
            content = content,
            timestamp = timestamp,
            type = type
        )
    }

    companion object {
        fun fromDomain(domain: ProtocolComment, protocolEntity: ProtocolEntity): ProtocolCommentEntity {
            return ProtocolCommentEntity(
                id = domain.id,
                protocol = protocolEntity,
                author = domain.author,
                content = domain.content,
                timestamp = domain.timestamp,
                type = domain.type
            )
        }
    }
}