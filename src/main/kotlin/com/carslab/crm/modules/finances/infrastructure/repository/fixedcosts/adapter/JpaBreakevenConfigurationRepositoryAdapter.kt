package com.carslab.crm.finances.infrastructure.repository.fixedcosts.adapter

import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfiguration
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfigurationId
import com.carslab.crm.finances.domain.ports.fixedcosts.BreakevenConfigurationRepository
import com.carslab.crm.finances.infrastructure.entity.BreakevenConfigurationEntity
import com.carslab.crm.finances.infrastructure.repository.fixedcosts.BreakevenConfigurationJpaRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import com.carslab.crm.infrastructure.security.SecurityContext

@Repository
class JpaBreakevenConfigurationRepositoryAdapter(
    private val breakevenConfigurationJpaRepository: BreakevenConfigurationJpaRepository,
    private val securityContext: SecurityContext,
) : BreakevenConfigurationRepository {

    @Transactional
    override fun save(configuration: BreakevenConfiguration): BreakevenConfiguration {
        val entity = BreakevenConfigurationEntity.Companion.fromDomain(configuration, companyId = securityContext.getCurrentCompanyId())
        val savedEntity = breakevenConfigurationJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: BreakevenConfigurationId): BreakevenConfiguration? {
        return breakevenConfigurationJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findActiveConfiguration(): BreakevenConfiguration? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return breakevenConfigurationJpaRepository.findByCompanyIdAndIsActiveTrue(companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(): List<BreakevenConfiguration> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return breakevenConfigurationJpaRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
            .map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: BreakevenConfigurationId): Boolean {
        return if (breakevenConfigurationJpaRepository.existsById(id.value)) {
            breakevenConfigurationJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    @Transactional
    override fun deactivateAll(): Int {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return breakevenConfigurationJpaRepository.deactivateAllForCompany(companyId)
    }
}