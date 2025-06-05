package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.*

/**
 * Simple signature request - without document, just signature collection
 */
data class SimpleSignatureRequest(
    @field:NotNull
    val tabletId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val signerName: String,

    @field:NotBlank
    @field:Size(max = 200)
    val signatureTitle: String,

    @field:Size(max = 1000)
    val instructions: String? = null,

    /**
     * Business context - can contain any domain-specific data
     * Examples: vehicle info, customer details, service info, etc.
     */
    val businessContext: Map<String, Any>? = null,

    @field:Positive
    @field:Max(60) // Max 1 hour for simple signatures
    val timeoutMinutes: Int = 10,

    /**
     * Optional reference to external entity (order, customer, etc.)
     */
    val externalReference: String? = null,

    /**
     * Type of signature being collected
     */
    val signatureType: SimpleSignatureType = SimpleSignatureType.GENERAL
)

/**
 * Simple signature response after successful collection
 */
data class SimpleSignatureResponse(
    val success: Boolean,
    val sessionId: UUID,
    val message: String,
    val expiresAt: Instant
)

/**
 * Simple signature submission from tablet
 */
data class SimpleSignatureSubmission(
    @field:NotBlank
    @field:Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    val sessionId: String,

    @field:NotBlank
    @field:Pattern(regexp = "^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")
    @field:Size(max = 5_000_000) // 5MB limit
    val signatureImage: String,

    @field:NotNull
    @field:PastOrPresent
    val signedAt: Instant,

    @field:NotNull
    val deviceId: UUID
)

/**
 * Simple signature completion response
 */
data class SimpleSignatureCompletionResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String,
    val signedAt: Instant? = null,
    val signatureImageUrl: String? = null
)

/**
 * Simple signature session for WebSocket
 */
data class SimpleSignatureSessionDto(
    @JsonProperty("session_id")
    val sessionId: UUID,

    @JsonProperty("tablet_id")
    val tabletId: UUID,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("signer_name")
    val signerName: String,

    @JsonProperty("signature_title")
    val signatureTitle: String,

    val instructions: String? = null,

    @JsonProperty("business_context")
    val businessContext: Map<String, Any>? = null,

    val status: SimpleSignatureStatus,

    @JsonProperty("created_at")
    val createdAt: Instant,

    @JsonProperty("expires_at")
    val expiresAt: Instant,

    @JsonProperty("signed_at")
    val signedAt: Instant? = null,

    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null,

    @JsonProperty("external_reference")
    val externalReference: String? = null,

    @JsonProperty("signature_type")
    val signatureType: SimpleSignatureType
)

/**
 * Simple signature request sent to tablet via WebSocket
 */
data class SimpleSignatureRequestDto(
    @JsonProperty("session_id")
    val sessionId: String,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("signer_name")
    val signerName: String,

    @JsonProperty("signature_title")
    val signatureTitle: String,

    val instructions: String? = null,

    @JsonProperty("business_context")
    val businessContext: Map<String, Any>? = null,

    @JsonProperty("timeout_minutes")
    val timeoutMinutes: Int,

    @JsonProperty("expires_at")
    val expiresAt: Instant,

    @JsonProperty("external_reference")
    val externalReference: String? = null,

    @JsonProperty("signature_type")
    val signatureType: SimpleSignatureType
)

/**
 * Types of simple signatures
 */
enum class SimpleSignatureType {
    GENERAL,
    ACKNOWLEDGMENT,
    AGREEMENT,
    RECEIPT,
    AUTHORIZATION,
    WITNESS,
    CUSTOM
}

/**
 * Simple signature session statuses
 */
enum class SimpleSignatureStatus {
    PENDING,
    SENT_TO_TABLET,
    IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    ERROR
}