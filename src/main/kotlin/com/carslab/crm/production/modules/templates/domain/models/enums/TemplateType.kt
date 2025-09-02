package com.carslab.crm.production.modules.templates.domain.models.enums

enum class TemplateType(val displayName: String) {
    SERVICE_AGREEMENT("Protokół przyjęcia pojazdu"),
    MARKETING_CONSENT("Zgoda marketingowa"),
    VEHICLE_PICKUP("Wydanie pojazdu"),
    MAIL_ON_VISIT_STARTED("Mail przy rozpoczęciu wizyty"),
    MAIL_ON_VISIT_COMPLETED("Mail po zakończeniu wizyty"),
    INVOICE("Faktura");

    companion object {
        fun fromString(it: String): TemplateType {
            return TemplateType.entries.firstOrNull { type -> type.name == it }
                ?: throw IllegalArgumentException("No enum constant for value: $it")
        }
    }

    fun getRequiredContentType(): String? {
        return when (this) {
            SERVICE_AGREEMENT, MARKETING_CONSENT, VEHICLE_PICKUP -> "application/pdf"
            MAIL_ON_VISIT_STARTED, MAIL_ON_VISIT_COMPLETED, INVOICE  -> "text/html"
        }
    }
}