package com.carslab.crm.signature.dto

import java.time.Instant
import java.util.UUID

data class SignatureResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String,
    val signedAt: Instant? = null,
    val documentUrl: String? = null
)

data class SignatureSessionResponse(
    val success: Boolean,
    val sessionId: String?,
    val message: String,
    val tabletId: UUID? = null,
    val workstationId: UUID? = null,
    val estimatedCompletionTime: java.time.Instant? = null
)