package com.carslab.crm.production.modules.invoice_templates.infrastructure.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "invoice_templates",
    indexes = [
        Index(name = "idx_invoice_templates_company_id", columnList = "company_id"),
        Index(name = "idx_invoice_templates_active", columnList = "company_id,is_active"),
        Index(name = "idx_invoice_templates_name", columnList = "company_id,name")
    ]
)
class InvoiceTemplateEntity(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    var htmlContent: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
)