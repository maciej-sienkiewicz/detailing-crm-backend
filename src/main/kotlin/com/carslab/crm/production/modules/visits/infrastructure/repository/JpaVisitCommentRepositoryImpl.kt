package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.model.VisitComment
import com.carslab.crm.production.modules.visits.domain.model.VisitId
import com.carslab.crm.production.modules.visits.domain.repository.VisitCommentRepository
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitCommentEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaVisitCommentRepositoryImpl(
    private val commentJpaRepository: VisitCommentJpaRepository
) : VisitCommentRepository {

    override fun save(comment: VisitComment): VisitComment {
        val entity = VisitCommentEntity.Companion.fromDomain(comment, comment.visitId.value)
        val savedEntity = commentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByVisitId(visitId: VisitId): List<VisitComment> {
        return commentJpaRepository.findByVisitIdOrderByCreatedAtDesc(visitId.value)
            .map { it.toDomain() }
    }

    override fun deleteById(commentId: String): Boolean {
        return if (commentJpaRepository.existsById(commentId)) {
            commentJpaRepository.deleteById(commentId)
            true
        } else {
            false
        }
    }
}