package com.carslab.crm.modules.visits.infrastructure.persistence.entity

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "protocol_comments_deprecated")
class ProtocolCommentEntityDeprecated(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Zmiana z obiektu na ID
    @Column(name = "protocol_id", nullable = false)
    var protocolId: Long,

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
            id = id!!,
            protocolId = ProtocolId(protocolId.toString()),
            author = author,
            content = content,
            timestamp = timestamp,
            type = type
        )
    }

    companion object {
        fun fromDomain(domain: ProtocolComment): ProtocolCommentEntityDeprecated {
            return ProtocolCommentEntityDeprecated(
                id = domain.id,
                protocolId = domain.protocolId.value.toLong(),
                author = domain.author,
                content = domain.content,
                timestamp = domain.timestamp,
                type = domain.type
            )
        }
    }
}