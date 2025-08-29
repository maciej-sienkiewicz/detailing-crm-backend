package com.carslab.crm.production.modules.templates.domain.models.aggregates

import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import java.time.LocalDateTime

data class Template(
    val id: String,
    val companyId: Long,
    val name: String,
    val type: TemplateType,
    val isActive: Boolean,
    val size: Long,
    val contentType: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank" }
        require(companyId > 0) { "Company ID must be positive" }
        require(size > 0) { "Template size must be positive" }
        require(contentType.isNotBlank()) { "Content type cannot be blank" }
    }

    fun updateMetadata(name: String, isActive: Boolean): Template {
        require(name.isNotBlank()) { "Template name cannot be blank" }

        return copy(
            name = name.trim(),
            isActive = isActive,
            updatedAt = LocalDateTime.now()
        )
    }

    fun activate(): Template = copy(isActive = true, updatedAt = LocalDateTime.now())
    fun deactivate(): Template = copy(isActive = false, updatedAt = LocalDateTime.now())
}
