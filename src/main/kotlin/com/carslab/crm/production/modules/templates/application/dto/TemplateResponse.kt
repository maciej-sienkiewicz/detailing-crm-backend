package com.carslab.crm.production.modules.templates.application.dto

import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.core.io.Resource
import java.time.LocalDateTime

data class TemplateResponse(
    val id: String,
    val name: String,
    val type: TemplateType,
    @JsonProperty("is_active")
    val isActive: Boolean,
    val size: Long,
    @JsonProperty("content_type")
    val contentType: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(template: Template): TemplateResponse {
            return TemplateResponse(
                id = template.id,
                name = template.name,
                type = template.type,
                isActive = template.isActive,
                size = template.size,
                contentType = template.contentType,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt
            )
        }
    }
}

data class TemplateTypeResponse(
    val type: TemplateType,
)

data class TemplateDownloadResponse(
    val resource: Resource,
    val contentType: String,
    val originalName: String
)