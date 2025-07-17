package com.carslab.crm.modules.invoice_templates.api.responses

import com.carslab.crm.modules.invoice_templates.domain.model.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class InvoiceTemplateResponse(
    val id: String,
    @JsonProperty("company_id")
    val companyId: Long,
    val name: String,
    val description: String?,
    @JsonProperty("template_type")
    val templateType: String,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("html_template")
    val htmlTemplate: String,
    @JsonProperty("css_styles")
    val cssStyles: String,
    @JsonProperty("logo_placement")
    val logoPlacement: LogoPlacementResponse,
    val layout: LayoutSettingsResponse,
    val metadata: TemplateMetadataResponse,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(template: InvoiceTemplate): InvoiceTemplateResponse {
            return InvoiceTemplateResponse(
                id = template.id.value,
                companyId = template.companyId,
                name = template.name,
                description = template.description,
                templateType = template.templateType.name,
                isActive = template.isActive,
                htmlTemplate = template.content.htmlTemplate,
                cssStyles = template.content.cssStyles,
                logoPlacement = LogoPlacementResponse.from(template.content.logoPlacement),
                layout = LayoutSettingsResponse.from(template.content.layout),
                metadata = TemplateMetadataResponse.from(template.metadata),
                createdAt = template.audit.createdAt,
                updatedAt = template.audit.updatedAt
            )
        }
    }
}

data class LogoPlacementResponse(
    val position: String,
    @JsonProperty("max_width")
    val maxWidth: Int,
    @JsonProperty("max_height")
    val maxHeight: Int
) {
    companion object {
        fun from(placement: LogoPlacement): LogoPlacementResponse {
            return LogoPlacementResponse(
                position = placement.position.name,
                maxWidth = placement.maxWidth,
                maxHeight = placement.maxHeight
            )
        }
    }
}

data class LayoutSettingsResponse(
    @JsonProperty("page_size")
    val pageSize: String,
    val margins: MarginsResponse,
    @JsonProperty("header_height")
    val headerHeight: Int?,
    @JsonProperty("footer_height")
    val footerHeight: Int?,
    @JsonProperty("font_family")
    val fontFamily: String,
    @JsonProperty("font_size")
    val fontSize: Int
) {
    companion object {
        fun from(layout: LayoutSettings): LayoutSettingsResponse {
            return LayoutSettingsResponse(
                pageSize = layout.pageSize.name,
                margins = MarginsResponse.from(layout.margins),
                headerHeight = layout.headerHeight,
                footerHeight = layout.footerHeight,
                fontFamily = layout.fontFamily,
                fontSize = layout.fontSize
            )
        }
    }
}

data class MarginsResponse(
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int
) {
    companion object {
        fun from(margins: Margins): MarginsResponse {
            return MarginsResponse(
                top = margins.top,
                right = margins.right,
                bottom = margins.bottom,
                left = margins.left
            )
        }
    }
}

data class TemplateMetadataResponse(
    val version: String,
    val author: String?,
    val tags: Set<String>,
    @JsonProperty("legal_compliance")
    val legalCompliance: LegalComplianceResponse,
    @JsonProperty("supported_languages")
    val supportedLanguages: Set<String>
) {
    companion object {
        fun from(metadata: TemplateMetadata): TemplateMetadataResponse {
            return TemplateMetadataResponse(
                version = metadata.version,
                author = metadata.author,
                tags = metadata.tags,
                legalCompliance = LegalComplianceResponse.from(metadata.legalCompliance),
                supportedLanguages = metadata.supportedLanguages
            )
        }
    }
}

data class LegalComplianceResponse(
    val country: String,
    @JsonProperty("vat_compliant")
    val vatCompliant: Boolean,
    @JsonProperty("required_fields")
    val requiredFields: Set<String>,
    @JsonProperty("last_legal_review")
    val lastLegalReview: LocalDateTime?
) {
    companion object {
        fun from(compliance: LegalCompliance): LegalComplianceResponse {
            return LegalComplianceResponse(
                country = compliance.country,
                vatCompliant = compliance.vatCompliant,
                requiredFields = compliance.requiredFields,
                lastLegalReview = compliance.lastLegalReview
            )
        }
    }
}