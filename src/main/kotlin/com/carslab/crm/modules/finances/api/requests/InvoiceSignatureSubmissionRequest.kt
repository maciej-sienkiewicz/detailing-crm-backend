package com.carslab.crm.modules.finances.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class InvoiceSignatureSubmissionRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    val sessionId: String,

    @field:NotBlank
    @field:Pattern(regexp = "^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")
    @field:Size(max = 5_000_000)
    val signatureImage: String,

    @field:NotBlank
    val deviceId: String,

    val signedAt: String? = null
)