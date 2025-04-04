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
@Transactional
class JpaProtocolCommentsRepositoryAdapter(
    private val protocolCommentJpaRepository: ProtocolCommentJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) : ProtocolCommentsRepository {

    override fun save(comment: ProtocolComment): ProtocolComment {
        val protocolEntity = protocolJpaRepository.findById(comment.protocolId.value).orElseThrow {
            IllegalStateException("Protocol with ID ${comment.protocolId.value} not found")
        }

        val commentEntity = ProtocolCommentEntity.fromDomain(comment, protocolEntity)
        val savedEntity = protocolCommentJpaRepository.save(commentEntity)

        return savedEntity.toDomain()
    }

    override fun findById(id: ProtocolId): List<ProtocolComment> {
        return protocolCommentJpaRepository.findByProtocol_Id(id.value.toLong()).map { it.toDomain() }
    }
}