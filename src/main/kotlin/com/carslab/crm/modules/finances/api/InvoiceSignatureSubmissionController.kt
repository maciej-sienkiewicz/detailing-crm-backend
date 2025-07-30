package com.carslab.crm.modules.finances.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureSubmissionRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureSubmissionResponse
import com.carslab.crm.modules.finances.domain.InvoiceSignatureService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/invoices/signature")
@Tag(name = "Invoice Signature Submission", description = "Submit invoice signatures from tablets")
class InvoiceSignatureSubmissionController(
    private val invoiceSignatureService: InvoiceSignatureService
) : BaseController() {

    @PostMapping("/submit")
    @Operation(summary = "Submit invoice signature from tablet", description = "Processes signature submission and updates invoice")
    fun submitInvoiceSignature(
        @RequestBody @Valid request: InvoiceSignatureSubmissionRequest
    ): ResponseEntity<InvoiceSignatureSubmissionResponse> {
        logger.info("Processing invoice signature submission for session: {}", request.sessionId)

        return try {
            val response = invoiceSignatureService.processSignatureFromTablet(
                request.sessionId,
                request.signatureImage
            )

            if (response) {
                ok(InvoiceSignatureSubmissionResponse(
                    success = true,
                    sessionId = request.sessionId,
                    message = "Invoice signature processed successfully"
                ))
            } else {
                badRequest("Failed to process invoice signature")
            }
        } catch (e: Exception) {
            logger.error("Error processing invoice signature submission", e)
            ResponseEntity.status(500).body(
                InvoiceSignatureSubmissionResponse(
                    success = false,
                    sessionId = request.sessionId,
                    message = "Failed to process signature: ${e.message}"
                )
            )
        }
    }
}