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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Signatures", description = "Zarządzanie podpisami faktur")
class InvoiceSignatureController(
    private val invoiceSignatureService: InvoiceSignatureService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/{invoiceId}/request-signature")
    @Operation(summary = "Poproś o podpis faktury")
    fun requestInvoiceSignature(
        @Parameter(description = "ID faktury") @PathVariable invoiceId: String,
        @RequestBody request: InvoiceSignatureRequest
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

    @GetMapping("/{invoiceId}/signature-status/{sessionId}")
    @Operation(summary = "Sprawdź status podpisu faktury")
    fun getInvoiceSignatureStatus(
        @Parameter(description = "ID faktury") @PathVariable invoiceId: String,
        @Parameter(description = "ID sesji podpisu") @PathVariable sessionId: UUID
    ): ResponseEntity<InvoiceSignatureStatusResponse> {
        logger.info("Getting signature status for invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()

        val response = invoiceSignatureService.getInvoiceSignatureStatus(
            sessionId = sessionId,
            companyId = companyId,
            invoiceId = invoiceId
        )

        return ok(response)
    }

    @GetMapping("/{invoiceId}/signed-document/{sessionId}")
    @Operation(summary = "Pobierz podpisaną fakturę")
    fun getSignedInvoice(
        @Parameter(description = "ID faktury") @PathVariable invoiceId: String,
        @Parameter(description = "ID sesji podpisu") @PathVariable sessionId: UUID
    ): ResponseEntity<ByteArray> {
        logger.info("Getting signed invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()

        val signedPdf = invoiceSignatureService.getSignedInvoice(
            sessionId = sessionId,
            companyId = companyId,
            invoiceId = invoiceId
        )

        return if (signedPdf != null) {
            ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"signed-invoice-$invoiceId.pdf\"")
                .body(signedPdf)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{invoiceId}/cancel-signature/{sessionId}")
    @Operation(summary = "Anuluj sesję podpisu faktury")
    fun cancelInvoiceSignature(
        @Parameter(description = "ID faktury") @PathVariable invoiceId: String,
        @Parameter(description = "ID sesji podpisu") @PathVariable sessionId: UUID,
        @RequestParam(required = false) reason: String?
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Cancelling signature session for invoice: {} session: {}", invoiceId, sessionId)

        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        invoiceSignatureService.cancelInvoiceSignatureSession(
            sessionId = sessionId,
            companyId = companyId,
            invoiceId = invoiceId,
            userId = userId.toString(),
            reason = reason
        )

        return ok(createSuccessResponse("Invoice signature session cancelled"))
    }
}