package com.carslab.crm.production.modules.visits.domain.models.enums

enum class DocumentType(val displayName: String) {
    ACCEPTANCE_PROTOCOL("Protokół odbioru"),
    MARKETING_CONSENT("Zgoda marketingowa"),
    SERVICE_CONSENT("Zgoda serwisowa"),
    INVOICE("Faktura"),
    RECEIPT("Paragon"),
    OTHER("Inny");

    companion object {
        fun fromString(value: String): DocumentType {
            return entries.find { it.name == value.uppercase() } ?: OTHER
        }
    }
}