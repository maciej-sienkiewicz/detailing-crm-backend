package com.carslab.crm.company_settings.domain.port

import com.carslab.crm.company_settings.domain.model.CompanySettings
import com.carslab.crm.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.company_settings.domain.model.CreateCompanySettings

interface CompanySettingsRepository {
    fun save(settings: CompanySettings): CompanySettings
    fun saveNew(settings: CreateCompanySettings): CompanySettings
    fun findByCompanyId(companyId: Long): CompanySettings?
    fun findById(id: CompanySettingsId): CompanySettings?
    fun existsByCompanyId(companyId: Long): Boolean
    fun deleteByCompanyId(companyId: Long): Boolean
}