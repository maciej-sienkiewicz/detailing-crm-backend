package com.carslab.crm.modules.company_settings.infrastructure.persistence.adapter

import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.modules.company_settings.domain.model.CreateCompanySettings
import com.carslab.crm.modules.company_settings.domain.port.CompanySettingsRepository
import com.carslab.crm.modules.company_settings.infrastructure.persistence.entity.CompanySettingsEntity
import com.carslab.crm.modules.company_settings.infrastructure.persistence.repository.CompanySettingsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class CompanySettingsRepositoryAdapter(
    private val companySettingsJpaRepository: CompanySettingsJpaRepository
) : CompanySettingsRepository {

    override fun save(settings: CompanySettings): CompanySettings {
        val existingEntity = companySettingsJpaRepository.findByCompanyIdAndActiveTrue(settings.companyId)
            .orElseThrow { IllegalArgumentException("Company settings not found") }

        updateEntityFromDomain(existingEntity, settings)
        val savedEntity = companySettingsJpaRepository.save(existingEntity)
        return savedEntity.toDomain()
    }

    override fun saveNew(settings: CreateCompanySettings): CompanySettings {
        val entity = CompanySettingsEntity.fromDomain(settings)
        val savedEntity = companySettingsJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long): CompanySettings? {
        return companySettingsJpaRepository.findByCompanyIdAndActiveTrue(companyId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun existsByCompanyId(companyId: Long): Boolean {
        return companySettingsJpaRepository.existsByCompanyIdAndActiveTrue(companyId)
    }

    override fun deleteByCompanyId(companyId: Long): Boolean {
        val deleted = companySettingsJpaRepository.softDeleteByCompanyId(companyId, LocalDateTime.now())
        return deleted > 0
    }

    private fun updateEntityFromDomain(entity: CompanySettingsEntity, domain: CompanySettings) {
        entity.companyName = domain.basicInfo.companyName
        entity.taxId = domain.basicInfo.taxId
        entity.address = domain.basicInfo.address
        entity.phone = domain.basicInfo.phone
        entity.website = domain.basicInfo.website

        entity.bankAccountNumber = domain.bankSettings.bankAccountNumber
        entity.bankName = domain.bankSettings.bankName
        entity.swiftCode = domain.bankSettings.swiftCode
        entity.accountHolderName = domain.bankSettings.accountHolderName

        entity.logoFileId = domain.logoSettings.logoFileId
        entity.logoFileName = domain.logoSettings.logoFileName
        entity.logoContentType = domain.logoSettings.logoContentType
        entity.logoSize = domain.logoSettings.logoSize
        entity.logoUrl = domain.logoSettings.logoUrl

        entity.updatedAt = domain.audit.updatedAt
        entity.updatedBy = domain.audit.updatedBy
    }
}