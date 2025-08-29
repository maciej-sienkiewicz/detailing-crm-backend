package com.carslab.crm.production.modules.templates.infrastructure.entity

import com.carslab.crm.production.modules.templates.domain.models.aggregates.Template
import com.carslab.crm.production.modules.templates.domain.models.enums.TemplateType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "templates",
    indexes = [
        Index(name = "idx_templates_company_id", columnList = "companyId"),
        Index(name = "idx_templates_type", columnList = "type"),
        Index(name = "idx_templates_active", columnList = "isActive"),
        Index(name = "idx_templates_company_type", columnList = "companyId, type"),
        Index(name = "idx_templates_company_active", columnList = "companyId, isActive")
    ]
)
class TemplateEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 255)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: TemplateType,

    @Column(nullable = false)
    val isActive: Boolean,

    @Column(nullable = false)
    val size: Long,

    @Column(nullable = false, length = 100)
    val contentType: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = false)
    val updatedAt: LocalDateTime
) {
    fun toDomain(): Template {
        return Template(
            id = id,
            companyId = companyId,
            name = name,
            type = type,
            isActive = isActive,
            size = size,
            contentType = contentType,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(template: Template): TemplateEntity {
            return TemplateEntity(
                id = template.id,
                companyId = template.companyId,
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