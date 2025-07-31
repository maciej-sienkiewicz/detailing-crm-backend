package com.carslab.crm.modules.finances.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.domain.InvoiceSignatureService
import com.carslab.crm.signature.service.SignatureException
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.servlet.ServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/invoice-signatures")
class InvoiceSignatureController(
    private val invoiceSignatureService: InvoiceSignatureService,
    private val securityContext: SecurityContext
) : BaseController() {
    
    @PostMapping("/request-from-visit")
    fun requestInvoiceSignatureFromVisit(
        @Valid @RequestBody request: InvoiceSignatureFromVisitRequest
    ): ResponseEntity<InvoiceSignatureResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            val response = invoiceSignatureService.requestInvoiceSignatureFromVisit(
                companyId = companyId,
                userId = userId.toString(),
                visitId = request.visitId,
                request = InvoiceSignatureRequest(
                    tabletId = request.tabletId,
                    customerName = request.customerName,
                    signatureTitle = request.signatureTitle,
                    instructions = request.instructions,
                    timeoutMinutes = request.timeoutMinutes
                )
            )

            ok(response)
        } catch (e: Exception) {
            logger.error("Error requesting invoice signature from visit", e)
            throw SignatureException("Failed to request invoice signature from visit: ${e.message}", e)
        }
    }

    @GetMapping("/sessions/{sessionId}/status")
    fun getInvoiceSignatureStatus(
        @PathVariable sessionId: UUID,
        @RequestParam invoice_id: String, servletRequest: ServletRequest
    ): ResponseEntity<InvoiceSignatureStatusResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val status = invoiceSignatureService.getInvoiceSignatureStatus(sessionId, companyId, invoice_id)
            if(status.status == InvoiceSignatureStatus.COMPLETED) {
                invoiceSignatureService.processSignatureFromTablet(sessionId.toString())
            }
            ok(status)
        } catch (e: Exception) {
            logger.error("Error getting invoice signature status", e)
            throw SignatureException("Failed to get signature status", e)
        }
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    fun cancelInvoiceSignature(
        @PathVariable sessionId: UUID,
        @RequestParam invoiceId: String,
        @RequestBody(required = false) reason: CancelInvoiceSignatureRequest? = null
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            invoiceSignatureService.cancelInvoiceSignatureSession(
                sessionId, companyId, invoiceId, userId.toString(), reason?.reason
            )

            ok(mapOf(
                "success" to true,
                "message" to "Invoice signature session cancelled successfully",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error cancelling invoice signature session", e)
            throw SignatureException("Failed to cancel session", e)
        }
    }

    @GetMapping("/sessions/{sessionId}/signed-document")
    fun getSignedInvoice(
        @PathVariable sessionId: UUID,
        @RequestParam invoiceId: String
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signedInvoice = invoiceSignatureService.getSignedInvoice(sessionId, companyId, invoiceId)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"signed-invoice-$invoiceId.pdf\"")
                .body(signedInvoice)
        } catch (e: Exception) {
            logger.error("Error getting signed invoice", e)
            throw SignatureException("Failed to get signed invoice", e)
        }
    }

    @GetMapping("/sessions/{sessionId}/signature-image")
    fun getSignatureImage(
        @PathVariable sessionId: UUID,
        @RequestParam invoiceId: String
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val cachedData = invoiceSignatureService.getCachedSignatureData(sessionId.toString())
                ?: return ResponseEntity.notFound().build()

            if (cachedData.companyId != companyId) {
                return ResponseEntity.notFound().build()
            }

            val contextInvoiceId = cachedData.metadata["invoiceId"] as? String
            if (contextInvoiceId != invoiceId) {
                return ResponseEntity.notFound().build()
            }

            if (cachedData.signatureImageBytes.isEmpty()) {
                return ResponseEntity.notFound().build()
            }

            ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .header("Content-Disposition", "inline; filename=\"signature-$sessionId.png\"")
                .body(cachedData.signatureImageBytes)
        } catch (e: Exception) {
            logger.error("Error getting signature image", e)
            throw SignatureException("Failed to get signature image", e)
        }
    }
}

data class InvoiceSignatureFromVisitRequest(
    @field:jakarta.validation.constraints.NotBlank
    @JsonProperty("visit_id")
    val visitId: String,

    @field:jakarta.validation.constraints.NotNull
    @JsonProperty("tablet_id")
    val tabletId: UUID,

    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Size(max = 200)
    @JsonProperty("customer_name")
    val customerName: String,

    @field:jakarta.validation.constraints.Size(max = 200)
    @JsonProperty("signature_title")
    val signatureTitle: String = "Podpis na fakturze",

    @field:jakarta.validation.constraints.Size(max = 1000)
    val instructions: String? = "Proszę podpisać fakturę",

    @field:jakarta.validation.constraints.Positive
    @field:jakarta.validation.constraints.Max(30)
    @JsonProperty("timeout_minutes")
    val timeoutMinutes: Int = 15
)

data class CancelInvoiceSignatureRequest(
    val reason: String? = null
)