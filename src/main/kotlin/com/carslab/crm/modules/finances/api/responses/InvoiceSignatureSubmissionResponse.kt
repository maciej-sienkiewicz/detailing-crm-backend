package com.carslab.crm.modules.finances.api.responses

import com.fasterxml.jackson.annotation.JsonProperty

data class InvoiceSignatureSubmissionResponse(
    val success: Boolean,
    @JsonProperty("session_id")
    val sessionId: String,
    val message: String,
    @JsonProperty("signed_at")
    val signedAt: String? = null,
    @JsonProperty("signed_invoice_url")
    val signedInvoiceUrl: String? = null
)