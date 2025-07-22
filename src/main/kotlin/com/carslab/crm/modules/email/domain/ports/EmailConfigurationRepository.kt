package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.modules.email.domain.model.EmailConfiguration
import com.carslab.crm.modules.email.domain.model.EmailConfigurationId

interface EmailConfigurationRepository {
    fun saveOrUpdate(configuration: EmailConfiguration): EmailConfiguration
    fun findByCompanyId(companyId: Long): EmailConfiguration?
    fun findById(id: EmailConfigurationId): EmailConfiguration?
    fun deleteByCompanyId(companyId: Long): Boolean
    fun existsByCompanyId(companyId: Long): Boolean
}