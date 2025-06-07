// src/main/kotlin/com/carslab/crm/signature/api/dto/SignatureDTOs.kt
package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.*

// ===== REQUEST DTOs =====

/**
 * Request to create signature session
 */
data class CreateSignatureSessionRequest(
    @field:NotNull
    val workstationId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val customerName: String,

    val vehicleInfo: VehicleInfoRequest? = null,

    @field:Size(max = 100)
    val serviceType: String? = null,

    @field:Size(max = 100)
    val documentType: String? = null,

    @field:Size(max = 1000)
    val additionalNotes: String? = null
)

/**
 * Vehicle information
 */
data class VehicleInfoRequest(
    @field:Size(max = 50)
    val make: String? = null,

    @field:Size(max = 50)
    val model: String? = null,

    @field:Size(max = 20)
    val licensePlate: String? = null,

    @field:Size(max = 17)
    val vin: String? = null,

    val year: Int? = null
)

/**
 * Signature submission from tablet
 */
data class SignatureSubmission(
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
 * Cancel session request
 */
data class CancelSessionRequest(
    val reason: String? = null
)

// ===== RESPONSE DTOs =====

/**
 * Response after creating signature session
 */
data class SignatureResponse(
    val success: Boolean,
    val sessionId: UUID,
    val message: String,
    val expiresAt: Instant
)

/**
 * Signature completion response
 */
data class SignatureCompletionResponse(
    val success: Boolean,
    @JsonProperty("session_id")
    val sessionId: String,
    val message: String,
    @JsonProperty("signed_at")
    val signedAt: Instant? = null,
    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null
)

// ===== DOMAIN DTOs =====

/**
 * Signature session information
 */
data class SignatureSessionDto(
    @JsonProperty("session_id")
    val sessionId: UUID,

    @JsonProperty("workstation_id")
    val workstationId: UUID? = null,

    @JsonProperty("tablet_id")
    val tabletId: UUID,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("customer_name")
    val customerName: String,

    @JsonProperty("vehicle_info")
    val vehicleInfo: VehicleInfoRequest? = null,

    @JsonProperty("service_type")
    val serviceType: String? = null,

    @JsonProperty("document_type")
    val documentType: String? = null,

    @JsonProperty("additional_notes")
    val additionalNotes: String? = null,

    val status: SignatureStatus,

    @JsonProperty("created_at")
    val createdAt: Instant,

    @JsonProperty("expires_at")
    val expiresAt: Instant,

    @JsonProperty("signed_at")
    val signedAt: Instant? = null,

    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null,

    @JsonProperty("has_signature")
    val hasSignature: Boolean = false
)

/**
 * Signature request sent to tablet via WebSocket
 */
data class SignatureRequestDto(
    @JsonProperty("session_id")
    val sessionId: String,

    @JsonProperty("customer_name")
    val customerName: String,

    @JsonProperty("vehicle_info")
    val vehicleInfo: VehicleInfoDto,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("document_type")
    val documentType: String,

    val timestamp: Instant = Instant.now()
)

/**
 * Vehicle info for WebSocket communication
 */
data class VehicleInfoDto(
    val make: String,
    val model: String,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val vin: String? = null
)

// ===== ENUMS =====

/**
 * Signature session statuses
 */
enum class SignatureStatus {
    PENDING,
    SENT_TO_TABLET,
    IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    ERROR
}