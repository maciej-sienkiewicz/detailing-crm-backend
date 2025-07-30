// src/main/kotlin/com/carslab/crm/modules/signature/api/controller/SignatureSubmissionController.kt
package com.carslab.crm.modules.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.signature.service.ProtocolSignatureService
import com.carslab.crm.signature.service.SignatureException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/signatures")
class SignatureSubmissionController(
    private val signatureService: ProtocolSignatureService,
) : BaseController() {

    /**
     * Submit a signature from tablet
     */
    @PostMapping("/submit")
    fun submitSignature(
        @Valid @RequestBody request: SignatureSubmissionRequest
    ): ResponseEntity<SignatureResponse> {

        return try {
            val response = signatureService.processSignatureFromTablet(
                request.sessionId,
                request.signatureImage
            )

            ok(SignatureResponse(
                success = true,
                sessionId = request.sessionId,
                message = "Signature submitted successfully",
                signedAt = request.signedAt,
                signatureImageUrl = "response.signatureImageUrl"
            ))
        } catch (e: Exception) {
            logger.error("Error submitting signature for session ${request.sessionId}", e)
            throw SignatureException("Failed to submit signature: ${e.message}", e)
        }
    }
}

// DTOs for signature submission
data class SignatureSubmissionRequest(
    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    val sessionId: String,

    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Pattern(regexp = "^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")
    @field:jakarta.validation.constraints.Size(max = 5_000_000) // 5MB limit
    val signatureImage: String,

    val signedAt: String, // ISO string

    @field:jakarta.validation.constraints.NotBlank
    val deviceId: String,
)

data class SignatureResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String,
    val signedAt: String? = null,
    val signatureImageUrl: String? = null
)