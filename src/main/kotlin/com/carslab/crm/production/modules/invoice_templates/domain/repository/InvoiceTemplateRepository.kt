package com.carslab.crm.production.modules.invoice_templates.domain.repository

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplateId

interface InvoiceTemplateRepository {
    fun save(template: InvoiceTemplate): InvoiceTemplate
    fun findById(id: InvoiceTemplateId): InvoiceTemplate?
    fun findByCompanyId(companyId: Long): List<InvoiceTemplate>
    fun findActiveTemplateForCompany(companyId: Long): InvoiceTemplate?
    fun existsByCompanyIdAndName(companyId: Long, name: String): Boolean
    fun deleteById(id: InvoiceTemplateId): Boolean
    fun deactivateAllForCompany(companyId: Long)
}