package com.carslab.crm.production.modules.invoice_templates.infrastructure.mapper

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplateId
import com.carslab.crm.production.modules.invoice_templates.infrastructure.entity.InvoiceTemplateEntity

fun InvoiceTemplate.toEntity(): InvoiceTemplateEntity {
    return InvoiceTemplateEntity(
        id = this.id.value,
        companyId = this.companyId,
        name = this.name,
        description = this.description,
        htmlContent = this.htmlContent,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}

fun InvoiceTemplateEntity.toDomain(): InvoiceTemplate {
    return InvoiceTemplate(
        id = InvoiceTemplateId.of(this.id),
        companyId = this.companyId,
        name = this.name,
        description = this.description,
        htmlContent = this.htmlContent,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}