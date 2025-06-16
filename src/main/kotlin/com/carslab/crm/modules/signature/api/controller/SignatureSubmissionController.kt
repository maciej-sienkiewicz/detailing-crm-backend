// src/main/kotlin/com/carslab/crm/modules/signature/api/controller/SignatureSubmissionController.kt
package com.carslab.crm.modules.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.service.SignatureSubmissionService
import com.carslab.crm.signature.service.SignatureException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.util.*

@RestController
@RequestMapping("/api/signatures")
class SignatureSubmissionController(
    private val signatureSubmissionService: SignatureSubmissionService,
    private val securityContext: SecurityContext
) : BaseController() {

    /**
     * Submit a signature from tablet
     */
    @PostMapping("/submit")
    fun submitSignature(
        @Valid @RequestBody request: SignatureSubmissionRequest
    ): ResponseEntity<SignatureResponse> {

        return try {
            val response = signatureSubmissionService.submitSignature(
                request = request,
            )

            ok(response)
        } catch (e: Exception) {
            logger.error("Error submitting signature for session ${request.sessionId}", e)
            throw SignatureException("Failed to submit signature: ${e.message}", e)
        }
    }

    /**
     * Get signed document download
     */
    @GetMapping("/{sessionId}/signed-document")
    fun getSignedDocument(
        @PathVariable sessionId: UUID
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signedDocument = signatureSubmissionService.getSignedDocument(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"signed-document-${sessionId}.pdf\"")
                .body(signedDocument)
        } catch (e: Exception) {
            logger.error("Error downloading signed document for session $sessionId", e)
            throw SignatureException("Failed to download signed document", e)
        }
    }

    /**
     * Get signature image
     */
    @GetMapping("/{sessionId}/signature-image")
    fun getSignatureImage(
        @PathVariable sessionId: UUID
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signatureImage = signatureSubmissionService.getSignatureImage(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .header("Content-Disposition", "inline; filename=\"signature-${sessionId}.png\"")
                .body(signatureImage)
        } catch (e: Exception) {
            logger.error("Error downloading signature image for session $sessionId", e)
            throw SignatureException("Failed to download signature image", e)
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