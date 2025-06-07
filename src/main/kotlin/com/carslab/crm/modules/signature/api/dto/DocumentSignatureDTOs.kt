package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.*

// ===== REQUEST DTOs =====

/**
 * Request to send document for signature to tablet
 */
data class SendDocumentForSignatureRequest(
    @field:NotNull
    val documentId: UUID,

    @field:NotNull
    val tabletId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val signerName: String,

    @field:NotBlank
    @field:Size(max = 100)
    val signatureTitle: String,

    @field:Size(max = 1000)
    val instructions: String? = null,

    /**
     * Business context metadata - flexible JSON structure
     * Examples: vehicle info, customer details, order info, etc.
     */
    val businessContext: Map<String, Any>? = null,

    @field:Positive
    @field:Max(120) // Max 2 hours
    val timeoutMinutes: Int = 15,

    /**
     * Where signature should be placed on document
     */
    val signatureFields: List<SignatureFieldDefinition>? = null
)

/**
 * Signature field definition for document
 */
data class SignatureFieldDefinition(
    val fieldId: String,
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val required: Boolean = true,
    val label: String? = null
)

/**
 * Document signature submission from tablet
 */
data class DocumentSignatureSubmission(
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
    val deviceId: UUID,

    /**
     * Signature placement information
     */
    val signaturePlacement: SignaturePlacement? = null
)

/**
 * Where signature was placed on document
 */
data class SignaturePlacement(
    val fieldId: String? = null,
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)


// ===== RESPONSE DTOs =====

/**
 * Response after sending document for signature
 */
data class SendDocumentForSignatureResponse(
    val success: Boolean,
    val sessionId: UUID,
    val message: String,
    val expiresAt: Instant
)

/**
 * Document upload response
 */
data class UploadDocumentResponse(
    val success: Boolean,
    val documentId: UUID,
    val message: String
)

/**
 * Document information response
 */
data class DocumentInfoResponse(
    val success: Boolean,
    val document: DocumentDto
)

/**
 * Signature session response
 */
data class SignatureSessionResponse(
    val success: Boolean,
    val session: DocumentSignatureSessionDto
)

/**
 * Session status response
 */
data class SessionStatusResponse(
    val success: Boolean,
    val sessionId: UUID,
    val status: SignatureSessionStatus,
    val timestamp: Instant
)

/**
 * Cancel session response
 */
data class CancelSessionResponse(
    val success: Boolean,
    val message: String
)

/**
 * Document signature response
 */
data class DocumentSignatureResponse(
    val success: Boolean,
    @JsonProperty("session_id")
    val sessionId: String,
    val message: String,
    @JsonProperty("signed_at")
    val signedAt: Instant? = null,
    @JsonProperty("signed_document_url")
    val signedDocumentUrl: String? = null,
    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null
)

// ===== DOMAIN DTOs =====

/**
 * Document information
 */
data class DocumentDto(
    val id: UUID,
    @JsonProperty("company_id")
    val companyId: Long,
    val title: String,
    @JsonProperty("document_type")
    val documentType: String,
    @JsonProperty("file_name")
    val fileName: String,
    @JsonProperty("file_size")
    val fileSize: Long,
    @JsonProperty("page_count")
    val pageCount: Int,
    @JsonProperty("upload_date")
    val uploadDate: Instant,
    @JsonProperty("uploaded_by")
    val uploadedBy: String,
    @JsonProperty("content_hash")
    val contentHash: String,
    val status: DocumentStatus,
    val metadata: Map<String, Any>? = null
)

/**
 * Document signature session
 */
data class DocumentSignatureSessionDto(
    @JsonProperty("session_id")
    val sessionId: UUID,

    @JsonProperty("document_id")
    val documentId: UUID,

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

    val status: SignatureSessionStatus,

    @JsonProperty("created_at")
    val createdAt: Instant,

    @JsonProperty("expires_at")
    val expiresAt: Instant,

    @JsonProperty("signed_at")
    val signedAt: Instant? = null,

    @JsonProperty("signature_image_url")
    val signatureImageUrl: String? = null,

    @JsonProperty("signed_document_url")
    val signedDocumentUrl: String? = null,

    @JsonProperty("has_signature")
    val hasSignature: Boolean = false,

    @JsonProperty("signature_fields")
    val signatureFields: List<SignatureFieldDefinition>? = null,

    @JsonProperty("signature_placement")
    val signaturePlacement: SignaturePlacement? = null
)

/**
 * Document signature request sent to tablet via WebSocket
 */
data class DocumentSignatureRequestDto(
    @JsonProperty("session_id")
    val sessionId: String,

    @JsonProperty("document_id")
    val documentId: String,

    @JsonProperty("company_id")
    val companyId: Long,

    @JsonProperty("signer_name")
    val signerName: String,

    @JsonProperty("signature_title")
    val signatureTitle: String,

    @JsonProperty("document_title")
    val documentTitle: String,

    @JsonProperty("document_type")
    val documentType: String,

    @JsonProperty("page_count")
    val pageCount: Int,

    @JsonProperty("preview_urls")
    val previewUrls: List<String>,

    val instructions: String? = null,

    @JsonProperty("business_context")
    val businessContext: Map<String, Any>? = null,

    @JsonProperty("timeout_minutes")
    val timeoutMinutes: Int,

    @JsonProperty("expires_at")
    val expiresAt: Instant,

    @JsonProperty("signature_fields")
    val signatureFields: List<SignatureFieldDefinition>? = null
)

// ===== ENUMS =====

/**
 * Document statuses
 */
enum class DocumentStatus {
    UPLOADED,
    PROCESSING,
    READY,
    ERROR,
    ARCHIVED
}

/**
 * Signature session statuses
 */
enum class SignatureSessionStatus {
    PENDING,
    SENT_TO_TABLET,
    VIEWING_DOCUMENT,
    SIGNING_IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    ERROR
}