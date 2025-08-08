// src/main/kotlin/com/carslab/crm/modules/company_settings/infrastructure/persistence/adapter/UserSignatureRepositoryAdapter.kt
package com.carslab.crm.modules.company_settings.infrastructure.persistence.adapter

import com.carslab.crm.modules.company_settings.domain.model.CreateUserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignatureId
import com.carslab.crm.modules.company_settings.domain.port.UserSignatureRepository
import com.carslab.crm.modules.company_settings.infrastructure.persistence.entity.UserSignatureEntity
import com.carslab.crm.modules.company_settings.infrastructure.persistence.repository.UserSignatureJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class UserSignatureRepositoryAdapter(
    private val userSignatureJpaRepository: UserSignatureJpaRepository
) : UserSignatureRepository {

    override fun save(signature: UserSignature): UserSignature {
        val existingEntity = userSignatureJpaRepository.findByUserIdAndCompanyIdAndActiveTrue(
            signature.userId,
            signature.companyId
        ).orElseThrow { IllegalArgumentException("User signature not found") }

        updateEntityFromDomain(existingEntity, signature)
        val savedEntity = userSignatureJpaRepository.save(existingEntity)
        return savedEntity.toDomain()
    }

    override fun saveNew(signature: CreateUserSignature): UserSignature {
        val entity = UserSignatureEntity.fromDomain(signature)
        val savedEntity = userSignatureJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByUserIdAndCompanyId(userId: Long, companyId: Long): UserSignature? {
        return userSignatureJpaRepository.findByUserIdAndCompanyIdAndActiveTrue(userId, companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UserSignatureId): UserSignature? {
        return userSignatureJpaRepository.findByIdAndActiveTrue(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun existsByUserIdAndCompanyId(userId: Long, companyId: Long): Boolean {
        return userSignatureJpaRepository.existsByUserIdAndCompanyIdAndActiveTrue(userId, companyId)
    }

    override fun deleteByUserIdAndCompanyId(userId: Long, companyId: Long): Boolean {
        val deleted = userSignatureJpaRepository.softDeleteByUserIdAndCompanyId(userId, companyId, LocalDateTime.now())
        return deleted > 0
    }

    private fun updateEntityFromDomain(entity: UserSignatureEntity, domain: UserSignature) {
        entity.content = domain.content
        entity.updatedAt = domain.audit.updatedAt
        entity.updatedBy = domain.audit.updatedBy
    }
}