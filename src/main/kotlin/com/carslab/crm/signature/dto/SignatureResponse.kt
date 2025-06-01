package com.carslab.crm.signature.dto

import java.time.Instant

data class SignatureResponse(
    val success: Boolean,
    val sessionId: String,
    val signedAt: Instant? = null,
    val message: String? = null
)

data class SignatureSessionResponse(
    val success: Boolean,
    val sessionId: String?,
    val expiresAt: Instant? = null,
    val message: String
)