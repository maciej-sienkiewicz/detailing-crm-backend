package com.carslab.crm.modules.finances.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.domain.InvoiceSignatureService
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Signatures", description = "Invoice signature management")
class InvoiceSignatureController(
    private val invoiceSignatureService: InvoiceSignatureService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/{invoiceId}/signature/request")
    @Operation(summary = "Request invoice signature", description = "Sends an invoice to tablet for signature")
    fun requestInvoiceSignature(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String,
        @RequestBody @Valid request: InvoiceSignatureRequest
    ): ResponseEntity<InvoiceSignatureResponse> {
        logger.info("Requesting signature for invoice: {}", invoiceId)

        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        val response = invoiceSignatureService.requestInvoiceSignature(
            companyId = companyId,
            userId = userId.toString(),
            invoiceId = invoiceId,
            request = request
        )

        return ok(response)
    }

    @GetMapping("/{invoiceId}/signature/{sessionId}/status")
    @Operation(summary = "Get signature status", description = "Gets the status of an invoice signature session")
    fun getInvoiceSignatureStatus(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String,
        @Parameter(description = "Session ID", required = true) @PathVariable sessionId: String
    ): ResponseEntity<InvoiceSignatureStatusResponse> {
        logger.info("Getting signature status for invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()

        val response = invoiceSignatureService.getInvoiceSignatureStatus(
            sessionId = UUID.fromString(sessionId),
            companyId = companyId,
            invoiceId = invoiceId
        )

        return ok(response)
    }

    @GetMapping("/{invoiceId}/signature/{sessionId}/signed-document")
    @Operation(summary = "Get signed invoice", description = "Downloads the signed invoice PDF")
    fun getSignedInvoice(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String,
        @Parameter(description = "Session ID", required = true) @PathVariable sessionId: String
    ): ResponseEntity<Resource> {
        logger.info("Getting signed invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()

        val signedPdf = invoiceSignatureService.getSignedInvoice(
            sessionId = UUID.fromString(sessionId),
            companyId = companyId,
            invoiceId = invoiceId
        )

        return if (signedPdf != null) {
            val resource = ByteArrayResource(signedPdf)
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signed-invoice-$invoiceId.pdf\"")
                .body(resource)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{invoiceId}/signature/{sessionId}/signature-image")
    @Operation(summary = "Get signature image", description = "Downloads the signature image")
    fun getSignatureImage(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String,
        @Parameter(description = "Session ID", required = true) @PathVariable sessionId: String
    ): ResponseEntity<Resource> {
        logger.info("Getting signature image for invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()
        val cachedData = invoiceSignatureService.getCachedSignatureData(sessionId)

        return if (cachedData?.companyId == companyId && cachedData.signatureImageBytes.isNotEmpty()) {
            val contextInvoiceId = cachedData.metadata["invoiceId"] as? String
            if (contextInvoiceId == invoiceId) {
                val resource = ByteArrayResource(cachedData.signatureImageBytes)
                ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"signature-$sessionId.png\"")
                    .body(resource)
            } else {
                ResponseEntity.notFound().build()
            }
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{invoiceId}/signature/{sessionId}")
    @Operation(summary = "Cancel signature session", description = "Cancels an active signature session")
    fun cancelInvoiceSignatureSession(
        @Parameter(description = "Invoice ID", required = true) @PathVariable invoiceId: String,
        @Parameter(description = "Session ID", required = true) @PathVariable sessionId: String,
        @Parameter(description = "Cancellation reason") @RequestParam(required = false) reason: String?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Cancelling signature session for invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        invoiceSignatureService.cancelInvoiceSignatureSession(
            sessionId = UUID.fromString(sessionId),
            companyId = companyId,
            invoiceId = invoiceId,
            userId = userId.toString(),
            reason = reason
        )

        return ok(createSuccessResponse("Invoice signature session cancelled successfully",
            mapOf("sessionId" to sessionId, "invoiceId" to invoiceId)))
    }
}