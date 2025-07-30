package com.carslab.crm.signature.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

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

data class SignaturePlacement(
    val fieldId: String? = null,
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

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
    val expiresAt: String,

    @JsonProperty("signature_fields")
    val signatureFields: List<SignatureFieldDefinition>? = null
)
enum class DocumentStatus {
    UPLOADED,
    ERROR,
}

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