package com.carslab.crm.modules.finances.api.responses

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class InvoiceSignatureResponse(
    val success: Boolean,
    @JsonProperty("session_id")
    val sessionId: UUID,
    val message: String,
    @JsonProperty("invoice_id")
    val invoiceId: String,
    @JsonProperty("expires_at")
    val expiresAt: Instant,
    @JsonProperty("document_preview_url")
    val documentPreviewUrl: String? = null
)

data class InvoiceSignatureStatusResponse(
    val success: Boolean,
    @JsonProperty("session_id")
    val sessionId: UUID,
    @JsonProperty("invoice_id")
    val invoiceId: String,
    val status: InvoiceSignatureStatus,
    @JsonProperty("signed_at")
    val signedAt: Instant? = null,
    @JsonProperty("signed_invoice_url")
    val signedInvoiceUrl: String? = null,
    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null,
    val timestamp: Instant
)

enum class InvoiceSignatureStatus {
    PENDING,
    SENT_TO_TABLET,
    VIEWING_INVOICE,
    SIGNING_IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    ERROR
}