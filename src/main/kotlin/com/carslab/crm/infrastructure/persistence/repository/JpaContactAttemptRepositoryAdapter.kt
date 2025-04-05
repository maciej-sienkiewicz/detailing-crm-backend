package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ContactAttempt
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.domain.model.ContactAttemptResult
import com.carslab.crm.domain.model.ContactAttemptType
import com.carslab.crm.domain.port.ContactAttemptRepository
import com.carslab.crm.infrastructure.persistence.entity.ContactAttemptEntity
import com.carslab.crm.infrastructure.persistence.repository.ContactAttemptJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class JpaContactAttemptRepositoryAdapter(
    private val contactAttemptJpaRepository: ContactAttemptJpaRepository
) : ContactAttemptRepository {

    override fun save(contactAttempt: ContactAttempt): ContactAttempt {
        val entity = if (contactAttempt.id.value.toLong() > 0 && contactAttemptJpaRepository.existsById(contactAttempt.id.value)) {
            val existingEntity = contactAttemptJpaRepository.findById(contactAttempt.id.value).get()

            existingEntity.clientId = contactAttempt.clientId
            existingEntity.date = contactAttempt.date
            existingEntity.type = contactAttempt.type
            existingEntity.description = contactAttempt.description
            existingEntity.result = contactAttempt.result
            existingEntity.updatedAt = contactAttempt.audit.updatedAt

            existingEntity
        } else {
            // Utworzenie nowej encji bez ID - baza danych wygeneruje ID
            ContactAttemptEntity(
                id = "0",
                clientId = contactAttempt.clientId,
                date = contactAttempt.date,
                type = contactAttempt.type,
                description = contactAttempt.description,
                result = contactAttempt.result,
                createdAt = contactAttempt.audit.createdAt,
                updatedAt = contactAttempt.audit.updatedAt
            )
        }

        val savedEntity = contactAttemptJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: ContactAttemptId): ContactAttempt? {
        return contactAttemptJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(): List<ContactAttempt> {
        return contactAttemptJpaRepository.findAll().map { it.toDomain() }
    }

    override fun deleteById(id: ContactAttemptId): Boolean {
        return if (contactAttemptJpaRepository.existsById(id.value)) {
            contactAttemptJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun findByClientId(clientId: String): List<ContactAttempt> {
        return contactAttemptJpaRepository.findByClientId(clientId).map { it.toDomain() }
    }

    override fun findByType(type: ContactAttemptType): List<ContactAttempt> {
        return contactAttemptJpaRepository.findByType(type).map { it.toDomain() }
    }

    override fun findByResult(result: ContactAttemptResult): List<ContactAttempt> {
        return contactAttemptJpaRepository.findByResult(result).map { it.toDomain() }
    }

    override fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<ContactAttempt> {
        return contactAttemptJpaRepository.findByDateBetween(startDate, endDate).map { it.toDomain() }
    }

    override fun findContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        return contactAttemptJpaRepository.findByClientId(clientId).map { it.toDomain() }
    }
}