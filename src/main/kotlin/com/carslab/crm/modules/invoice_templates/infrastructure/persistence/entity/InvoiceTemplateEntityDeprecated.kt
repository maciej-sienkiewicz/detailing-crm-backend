package com.carslab.crm.modules.invoice_templates.infrastructure.persistence.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.modules.invoice_templates.domain.model.*
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "invoice_templates_deprecated",
    indexes = [
        Index(name = "idx_invoice_templates_company_id_deprecated", columnList = "company_id"),
        Index(name = "idx_invoice_templates_type_deprecated", columnList = "template_type"),
    ]
)
class InvoiceTemplateEntityDeprecated(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    var templateType: TemplateType,

    @Column(name = "html_template", nullable = false, columnDefinition = "TEXT")
    var htmlTemplate: String,

    @Column(name = "css_styles", nullable = false, columnDefinition = "TEXT")
    var cssStyles: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    // Layout settings
    @Enumerated(EnumType.STRING)
    @Column(name = "page_size", nullable = false)
    var pageSize: PageSize = PageSize.A4,

    @Column(name = "margin_top", nullable = false)
    var marginTop: Int = 20,

    @Column(name = "margin_right", nullable = false)
    var marginRight: Int = 20,

    @Column(name = "margin_bottom", nullable = false)
    var marginBottom: Int = 20,

    @Column(name = "margin_left", nullable = false)
    var marginLeft: Int = 20,

    @Column(name = "font_family", nullable = false)
    var fontFamily: String = "DejaVuSans",

    @Column(name = "font_size", nullable = false)
    var fontSize: Int = 12,

    // Logo settings
    @Enumerated(EnumType.STRING)
    @Column(name = "logo_position", nullable = false)
    var logoPosition: LogoPosition = LogoPosition.TOP_LEFT,

    @Column(name = "logo_max_width", nullable = false)
    var logoMaxWidth: Int = 150,

    @Column(name = "logo_max_height", nullable = false)
    var logoMaxHeight: Int = 80,

    // Metadata
    @Column(name = "template_version", nullable = false)
    var templateVersion: String = "1.0",

    @Column(name = "author")
    var author: String? = null,

    @Column(name = "tags")
    var tags: String? = null,

    @Column(name = "country", nullable = false)
    var country: String = "PL",

    @Column(name = "vat_compliant", nullable = false)
    var vatCompliant: Boolean = true,

    @Column(name = "supported_languages")
    var supportedLanguages: String = "pl",

    // Audit
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun toDomain(): InvoiceTemplate {
        return InvoiceTemplate(
            id = InvoiceTemplateId(id),
            companyId = companyId,
            name = name,
            description = description,
            templateType = templateType,
            content = TemplateContent(
                htmlTemplate = htmlTemplate,
                cssStyles = cssStyles,
                logoPlacement = LogoPlacement(
                    position = logoPosition,
                    maxWidth = logoMaxWidth,
                    maxHeight = logoMaxHeight
                ),
                layout = LayoutSettings(
                    pageSize = pageSize,
                    margins = Margins(
                        top = marginTop,
                        right = marginRight,
                        bottom = marginBottom,
                        left = marginLeft
                    ),
                    headerHeight = null,
                    footerHeight = null,
                    fontFamily = fontFamily,
                    fontSize = fontSize
                )
            ),
            isActive = isActive,
            metadata = TemplateMetadata(
                version = templateVersion,
                author = author,
                tags = tags?.split(",")?.map { it.trim() }?.toSet() ?: emptySet(),
                legalCompliance = LegalCompliance(
                    country = country,
                    vatCompliant = vatCompliant,
                    requiredFields = getRequiredFields(),
                    lastLegalReview = null
                ),
                supportedLanguages = supportedLanguages.split(",").map { it.trim() }.toSet()
            ),
            audit = Audit(
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )
    }

    private fun getRequiredFields(): Set<String> {
        return setOf(
            "invoice_number", "issue_date", "due_date",
            "seller_name", "seller_address", "seller_tax_id",
            "buyer_name", "buyer_address", "buyer_tax_id",
            "items", "total_net", "total_tax", "total_gross"
        )
    }

    companion object {
        fun fromDomain(template: InvoiceTemplate): InvoiceTemplateEntityDeprecated {
            return InvoiceTemplateEntityDeprecated(
                id = template.id.value,
                companyId = template.companyId,
                name = template.name,
                description = template.description,
                templateType = template.templateType,
                htmlTemplate = template.content.htmlTemplate,
                cssStyles = template.content.cssStyles,
                isActive = template.isActive,
                pageSize = template.content.layout.pageSize,
                marginTop = template.content.layout.margins.top,
                marginRight = template.content.layout.margins.right,
                marginBottom = template.content.layout.margins.bottom,
                marginLeft = template.content.layout.margins.left,
                fontFamily = template.content.layout.fontFamily,
                fontSize = template.content.layout.fontSize,
                logoPosition = template.content.logoPlacement.position,
                logoMaxWidth = template.content.logoPlacement.maxWidth,
                logoMaxHeight = template.content.logoPlacement.maxHeight,
                templateVersion = template.metadata.version,
                author = template.metadata.author,
                tags = template.metadata.tags.joinToString(","),
                country = template.metadata.legalCompliance.country,
                vatCompliant = template.metadata.legalCompliance.vatCompliant,
                supportedLanguages = template.metadata.supportedLanguages.joinToString(","),
                createdAt = template.audit.createdAt,
                updatedAt = template.audit.updatedAt
            )
        }
    }
}