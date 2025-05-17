package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.port.ProtocolCommentsRepository
import com.carslab.crm.infrastructure.persistence.entity.ProtocolCommentEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolCommentJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaProtocolCommentsRepositoryAdapter(
    private val protocolCommentJpaRepository: ProtocolCommentJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolCommentsRepository {

    override fun save(comment: ProtocolComment): ProtocolComment {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Sprawdź, czy protokół istnieje i należy do tej samej firmy
        val protocolId = comment.protocolId.value.toLong()
        val protocol = protocolJpaRepository.findByCompanyIdAndId(companyId, comment.protocolId.value.toLong())
            .orElse(null) ?: throw IllegalStateException("Protocol with ID ${comment.protocolId.value} not found or access denied")

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
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Najpierw sprawdź, czy protokół należy do firmy
        val protocol = protocolJpaRepository.findByCompanyIdAndId(companyId, id.value.toLong())
            .orElse(null) ?: return emptyList()

        // Pobierz komentarze dla protokołu
        return protocolCommentJpaRepository.findByProtocolId(id.value.toLong())
            .map { it.toDomain() }
    }
}