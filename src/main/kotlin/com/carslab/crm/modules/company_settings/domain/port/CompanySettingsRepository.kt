package com.carslab.crm.modules.company_settings.domain.port

import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.modules.company_settings.domain.model.CreateCompanySettings

interface CompanySettingsRepository {
    fun save(settings: CompanySettings): CompanySettings
    fun saveNew(settings: CreateCompanySettings): CompanySettings
    fun findByCompanyId(companyId: Long): CompanySettings?
    fun existsByCompanyId(companyId: Long): Boolean
    fun deleteByCompanyId(companyId: Long): Boolean
}