package com.carslab.crm.production.modules.invoice_templates.application.dto

import com.carslab.crm.production.modules.invoice_templates.domain.model.InvoiceTemplate
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class InvoiceTemplateHeaderResponse(
    val id: String,
    val name: String,
    val description: String?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(template: InvoiceTemplate): InvoiceTemplateHeaderResponse {
            return InvoiceTemplateHeaderResponse(
                id = template.id.value,
                name = template.name,
                description = template.description,
                isActive = template.isActive,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt
            )
        }
    }
}

data class InvoiceTemplateResponse(
    val header: InvoiceTemplateHeaderResponse,
    val htmlContent: String,
)