package com.carslab.crm.production.modules.companysettings.domain.repository

import com.carslab.crm.production.modules.companysettings.domain.model.Company

interface CompanyRepository {
    fun save(company: Company): Company
    fun existsByTaxId(taxId: String): Boolean
    fun findById(companyId: Long): Company?
}