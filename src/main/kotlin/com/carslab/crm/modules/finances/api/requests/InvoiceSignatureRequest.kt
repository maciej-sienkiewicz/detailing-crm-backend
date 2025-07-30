package com.carslab.crm.modules.finances.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class InvoiceSignatureRequest(
    @JsonProperty("tablet_id")
    val tabletId: UUID,
    @JsonProperty("customer_name")
    val customerName: String,
    @JsonProperty("signature_title")
    val signatureTitle: String = "Podpis faktury",
    @JsonProperty("instructions")
    val instructions: String? = null,
    @JsonProperty("timeout_minutes")
    val timeoutMinutes: Int = 10
)