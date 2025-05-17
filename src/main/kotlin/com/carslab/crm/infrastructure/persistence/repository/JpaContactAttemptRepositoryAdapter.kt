package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.port.ContactAttemptRepository
import com.carslab.crm.infrastructure.persistence.entity.ContactAttemptEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ContactAttemptJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class JpaContactAttemptRepositoryAdapter(
    private val contactAttemptJpaRepository: ContactAttemptJpaRepository
) : ContactAttemptRepository {

    override fun save(contactAttempt: ContactAttempt): ContactAttempt {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (contactAttempt.id.value.isNotEmpty() &&
            contactAttemptJpaRepository.findByCompanyIdAndId(companyId, contactAttempt.id.value).isPresent) {
            val existingEntity = contactAttemptJpaRepository.findByCompanyIdAndId(companyId, contactAttempt.id.value).get()

            existingEntity.clientId = contactAttempt.clientId
            existingEntity.date = contactAttempt.date
            existingEntity.type = contactAttempt.type
            existingEntity.description = contactAttempt.description
            existingEntity.result = contactAttempt.result
            existingEntity.updatedAt = contactAttempt.audit.updatedAt

            existingEntity
        } else {
            ContactAttemptEntity.fromDomain(contactAttempt)
        }

        val savedEntity = contactAttemptJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: ContactAttemptId): ContactAttempt? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }

    override fun deleteById(id: ContactAttemptId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = contactAttemptJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        contactAttemptJpaRepository.delete(entity)
        return true
    }

    override fun findByClientId(clientId: String): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByClientIdAndCompanyId(clientId, companyId)
            .map { it.toDomain() }
    }

    override fun findByType(type: ContactAttemptType): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByTypeAndCompanyId(type, companyId)
            .map { it.toDomain() }
    }

    override fun findByResult(result: ContactAttemptResult): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByResultAndCompanyId(result, companyId)
            .map { it.toDomain() }
    }

    override fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByDateBetweenAndCompanyId(startDate, endDate, companyId)
            .map { it.toDomain() }
    }

    override fun findContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return contactAttemptJpaRepository.findByClientIdAndCompanyId(clientId, companyId)
            .map { it.toDomain() }
    }
}