package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.port.ProtocolCommentsRepository
import com.carslab.crm.infrastructure.persistence.entity.ProtocolCommentEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolCommentJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaProtocolCommentsRepositoryAdapter(
    private val protocolCommentJpaRepository: ProtocolCommentJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolCommentsRepository {

    override fun save(comment: ProtocolComment): ProtocolComment {
        // Sprawdź, czy protokół istnieje
        val protocolId = comment.protocolId.value.toLong()
        if (!protocolJpaRepository.existsById(comment.protocolId.value)) {
            throw IllegalStateException("Protocol with ID ${comment.protocolId.value} not found")
        }

        val commentEntity = ProtocolCommentEntity(
            protocolId = protocolId,
            author = comment.author,
            content = comment.content,
            timestamp = comment.timestamp,
            type = comment.type
        )

        val savedEntity = protocolCommentJpaRepository.save(commentEntity)
        return savedEntity.toDomain()
    }

    override fun findById(id: ProtocolId): List<ProtocolComment> {
        // Zmienione wywołanie, aby używać metod używających identyfikatorów zamiast relacji
        return protocolCommentJpaRepository.findByProtocolId(id.value.toLong())
            .map { it.toDomain() }
    }
}