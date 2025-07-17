package com.carslab.crm.modules.invoice_templates.domain.ports

import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.modules.invoice_templates.domain.model.TemplateType
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.model.LayoutSettings

interface InvoiceTemplateRepository {
    fun save(template: InvoiceTemplate): InvoiceTemplate
    fun findById(id: InvoiceTemplateId): InvoiceTemplate?
    fun findByCompanyId(companyId: Long): List<InvoiceTemplate>
    fun findActiveTemplateForCompany(companyId: Long): InvoiceTemplate?
    fun findSystemDefaultTemplate(): InvoiceTemplate?
    fun deactivateAllTemplatesForCompany(companyId: Long)
    fun findByType(type: TemplateType): List<InvoiceTemplate>
    fun deleteById(id: InvoiceTemplateId): Boolean
}