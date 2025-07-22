package com.carslab.crm.modules.email.infrastructure.persistence.repository.adapter

import com.carslab.crm.modules.email.domain.model.EmailConfiguration
import com.carslab.crm.modules.email.domain.model.EmailConfigurationId
import com.carslab.crm.modules.email.domain.ports.EmailConfigurationRepository
import com.carslab.crm.modules.email.infrastructure.persistence.entity.EmailConfigurationEntity
import com.carslab.crm.modules.email.infrastructure.persistence.repository.EmailConfigurationJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class EmailConfigurationRepositoryAdapter(
    private val emailConfigurationJpaRepository: EmailConfigurationJpaRepository
) : EmailConfigurationRepository {

    override fun saveOrUpdate(configuration: EmailConfiguration): EmailConfiguration {
        val existingEntity = emailConfigurationJpaRepository.findByCompanyId(configuration.companyId)

        val entity = if (existingEntity.isPresent) {
            val existing = existingEntity.get()
            updateEntityFromDomain(existing, configuration)
            existing
        } else {
            EmailConfigurationEntity.fromDomain(configuration)
        }

        val savedEntity = emailConfigurationJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long): EmailConfiguration? {
        return emailConfigurationJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findById(id: EmailConfigurationId): EmailConfiguration? {
        return emailConfigurationJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun deleteByCompanyId(companyId: Long): Boolean {
        val deleted = emailConfigurationJpaRepository.deleteByCompanyId(companyId)
        return deleted > 0
    }

    @Transactional(readOnly = true)
    override fun existsByCompanyId(companyId: Long): Boolean {
        return emailConfigurationJpaRepository.existsByCompanyId(companyId)
    }

    private fun updateEntityFromDomain(entity: EmailConfigurationEntity, domain: EmailConfiguration) {
        entity.senderEmail = domain.senderEmail
        entity.senderName = domain.senderName
        entity.encryptedPassword = domain.encryptedPassword
        entity.smtpHost = domain.smtpHost
        entity.smtpPort = domain.smtpPort
        entity.useSSL = domain.useSSL
        entity.isEnabled = domain.isEnabled
        entity.validationStatus = domain.validationStatus
        entity.validationMessage = domain.validationMessage
        entity.providerHint = domain.providerHint
        entity.updatedAt = LocalDateTime.now()
    }
}