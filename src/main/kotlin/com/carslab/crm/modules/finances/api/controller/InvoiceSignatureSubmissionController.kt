package com.carslab.crm.modules.finances.api.controller

data class InvoiceSignatureSubmissionRequest(
    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    val sessionId: String,

    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Pattern(regexp = "^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")
    @field:jakarta.validation.constraints.Size(max = 5_000_000)
    val signatureImage: String,

    @field:jakarta.validation.constraints.NotBlank
    val deviceId: String
)

data class InvoiceSignatureSubmissionResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String,
    val timestamp: String
)