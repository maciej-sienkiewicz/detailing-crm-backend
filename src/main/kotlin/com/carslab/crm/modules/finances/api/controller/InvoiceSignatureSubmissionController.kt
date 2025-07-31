package com.carslab.crm.modules.finances.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.finances.domain.InvoiceSignatureService
import com.carslab.crm.signature.service.SignatureException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.Instant

@RestController
@RequestMapping("/api/invoice-signatures")
class InvoiceSignatureSubmissionController(
    private val invoiceSignatureService: InvoiceSignatureService
) : BaseController() {

    @PostMapping("/submit")
    fun submitInvoiceSignature(
        @Valid @RequestBody request: InvoiceSignatureSubmissionRequest
    ): ResponseEntity<InvoiceSignatureSubmissionResponse> { 

        return try {
            val success = invoiceSignatureService.processSignatureFromTablet(
                request.sessionId,
                request.signatureImage
            )

            ok(InvoiceSignatureSubmissionResponse(
                success = success,
                sessionId = request.sessionId,
                message = if (success) "Invoice signature submitted successfully" else "Failed to process signature",
                timestamp = Instant.now().toString()
            ))
        } catch (e: Exception) {
            logger.error("Error submitting invoice signature for session ${request.sessionId}", e)
            throw SignatureException("Failed to submit invoice signature: ${e.message}", e)
        }
    }
}

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