package com.carslab.crm.modules.email.infrastructure.persistence.repository.adapter

import com.carslab.crm.modules.email.domain.model.EmailHistory
import com.carslab.crm.modules.email.domain.model.EmailHistoryId
import com.carslab.crm.modules.email.domain.model.EmailStatus
import com.carslab.crm.modules.email.domain.ports.EmailRepository
import com.carslab.crm.modules.email.infrastructure.persistence.entity.EmailHistoryEntity
import com.carslab.crm.modules.email.infrastructure.persistence.repository.EmailHistoryJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import org.springframework.stereotype.Repository

@Repository
class JpaEmailRepositoryAdapter(
    private val emailHistoryJpaRepository: EmailHistoryJpaRepository,
    private val securityContext: SecurityContext
) : EmailRepository {

    override fun save(emailHistory: EmailHistory): EmailHistory {
        val entity = EmailHistoryEntity.fromDomain(emailHistory)
        val savedEntity = emailHistoryJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: EmailHistoryId): EmailHistory? {
        return emailHistoryJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun updateStatus(id: EmailHistoryId, status: EmailStatus, errorMessage: String?, authContext: AuthContext?) {
        val companyId = authContext?.companyId?.value ?: securityContext.getCurrentCompanyId()
        emailHistoryJpaRepository.findById(id.value).ifPresent { entity ->
            if (entity.companyId == companyId) {
                entity.status = status
                entity.errorMessage = errorMessage
                emailHistoryJpaRepository.save(entity)
            }
        }
    }
}