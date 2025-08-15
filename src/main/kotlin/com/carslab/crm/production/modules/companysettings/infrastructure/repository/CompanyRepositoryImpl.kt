package com.carslab.crm.production.modules.companysettings.infrastructure.repository

import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.carslab.crm.production.modules.companysettings.domain.repository.CompanyRepository
import com.carslab.crm.production.modules.companysettings.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.companysettings.infrastructure.mapper.toEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class CompanyRepositoryImpl(
    private val jpaRepository: CompanyJpaRepository
) : CompanyRepository {

    override fun save(company: Company): Company {
        val entity = company.toEntity()
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun existsByTaxId(taxId: String): Boolean {
        return jpaRepository.existsByTaxIdAndActiveTrue(taxId)
    }
    
    @Transactional(readOnly = true)
    override fun findById(companyId: Long): Company? {
        return jpaRepository.findByIdAndActiveTrue(companyId)
            .map { it.toDomain() }
            .orElse(null)
    }
}