package com.carslab.crm.production.modules.visits.domain.model

import java.time.LocalDateTime

data class VisitDocument(
    val id: String,
    val visitId: VisitId,
    val name: String,
    val type: DocumentType,
    val size: Long,
    val contentType: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val uploadedBy: String
)

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