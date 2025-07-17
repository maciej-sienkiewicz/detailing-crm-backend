package com.carslab.crm.modules.invoice_templates.domain.model

import com.carslab.crm.domain.model.Audit
import java.time.LocalDateTime
import java.util.*

data class InvoiceTemplate(
    val id: InvoiceTemplateId,
    val companyId: Long,
    val name: String,
    val description: String?,
    val templateType: TemplateType,
    val content: TemplateContent,
    val isActive: Boolean,
    val metadata: TemplateMetadata,
    val audit: Audit
) {
    fun canBeUsedBy(companyId: Long): Boolean {
        return this.companyId == companyId || templateType == TemplateType.SYSTEM_DEFAULT
    }
}

@JvmInline
value class InvoiceTemplateId(val value: String) {
    companion object {
        fun generate(): InvoiceTemplateId = InvoiceTemplateId(UUID.randomUUID().toString())
    }
}

enum class TemplateType(val displayName: String) {
    SYSTEM_DEFAULT("Domyślny systemowy"),
    COMPANY_CUSTOM("Własny firmy")
}

data class TemplateContent(
    val htmlTemplate: String,
    val cssStyles: String,
    val logoPlacement: LogoPlacement,
    val layout: LayoutSettings
)

data class LogoPlacement(
    val position: LogoPosition,
    val maxWidth: Int,
    val maxHeight: Int
)

enum class LogoPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, HEADER_LEFT, HEADER_RIGHT
}

data class LayoutSettings(
    val pageSize: PageSize,
    val margins: Margins,
    val headerHeight: Int?,
    val footerHeight: Int?,
    val fontFamily: String,
    val fontSize: Int
)

enum class PageSize {
    A4, A5, LETTER
}

data class Margins(
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int
)

data class TemplateMetadata(
    val version: String,
    val author: String?,
    val tags: Set<String>,
    val legalCompliance: LegalCompliance,
    val supportedLanguages: Set<String>
)

data class LegalCompliance(
    val country: String,
    val vatCompliant: Boolean,
    val requiredFields: Set<String>,
    val lastLegalReview: LocalDateTime?
)

data class InvoiceGenerationData(
    val document: com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument,
    val companySettings: com.carslab.crm.modules.company_settings.domain.model.CompanySettings,
    val logoData: ByteArray?,
    val additionalData: Map<String, Any> = emptyMap()
)