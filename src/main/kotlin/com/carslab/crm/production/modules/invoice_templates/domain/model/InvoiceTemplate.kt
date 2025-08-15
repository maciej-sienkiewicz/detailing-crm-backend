package com.carslab.crm.production.modules.invoice_templates.domain.model

import java.time.LocalDateTime
import java.util.*

@JvmInline
value class InvoiceTemplateId(val value: String) {
    companion object {
        fun generate(): InvoiceTemplateId = InvoiceTemplateId(UUID.randomUUID().toString())
        fun of(value: String): InvoiceTemplateId = InvoiceTemplateId(value)
    }
}

data class InvoiceTemplate(
    val id: InvoiceTemplateId,
    val companyId: Long,
    val name: String,
    val description: String?,
    val htmlContent: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long
) {
    fun canBeUsedBy(companyId: Long): Boolean {
        return this.companyId == companyId
    }

    fun activate(): InvoiceTemplate {
        return copy(
            isActive = true,
            updatedAt = LocalDateTime.now(),
            version = version + 1
        )
    }

    fun deactivate(): InvoiceTemplate {
        return copy(
            isActive = false,
            updatedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
}