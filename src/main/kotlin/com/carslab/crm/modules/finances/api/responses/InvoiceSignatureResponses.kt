package com.carslab.crm.modules.finances.api.responses

import java.time.Instant
import java.util.UUID

data class InvoiceSignatureResponse(
    val success: Boolean,
    val sessionId: UUID,
    val message: String,
    val invoiceId: String,
    val expiresAt: Instant,
    val invoicePreviewUrl: String? = null
)

data class InvoiceSignatureStatusResponse(
    val success: Boolean,
    val sessionId: UUID,
    val invoiceId: String,
    val status: InvoiceSignatureStatus,
    val signedAt: Instant? = null,
    val signedInvoiceUrl: String? = null,
    val signatureImageUrl: String? = null,
    val timestamp: Instant
)

enum class InvoiceSignatureStatus(val displayName: String) {
    PENDING("Oczekuje"),
    SENT_TO_TABLET("Wysłano do tableta"),
    VIEWING_INVOICE("Przeglądanie faktury"),
    SIGNING_IN_PROGRESS("Podpisywanie w toku"),
    COMPLETED("Zakończono"),
    EXPIRED("Wygasło"),
    CANCELLED("Anulowano"),
    ERROR("Błąd")
}