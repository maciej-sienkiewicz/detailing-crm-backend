package com.carslab.crm.modules.finances.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.domain.model.UserId
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.domain.signature.InvoiceSignatureOrchestrator
import com.carslab.crm.modules.finances.domain.signature.model.InvoiceSignatureException
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.OverridenInvoiceServiceItem
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.lang.IllegalStateException
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/invoice-signatures")
class InvoiceSignatureController(
    private val orchestrator: InvoiceSignatureOrchestrator,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/request-from-visit")
    fun requestInvoiceSignatureFromVisit(
        @Valid @RequestBody request: InvoiceSignatureFromVisitRequest
    ): ResponseEntity<InvoiceSignatureResponse> {
        val authContext = AuthContext(
            companyId = CompanyId(securityContext.getCurrentCompanyId()),
            userId = UserId(securityContext.getCurrentUserId() ?: throw IllegalStateException("User ID is null")),
            userName = securityContext.getCurrentUserName() ?: "Unknown"
        )

        return try {
            val signatureRequest = InvoiceSignatureRequest(
                tabletId = request.tabletId,
                customerName = request.customerName,
                signatureTitle = request.signatureTitle,
                instructions = request.instructions,
                timeoutMinutes = request.timeoutMinutes,
                overridenItems = request.overridenItems.map {
                    OverridenInvoiceServiceItem(
                        name = it.name,
                        quantity = it.quantity,
                        basePrice = it.price,
                        discountType = it.discountType.toString(),
                        discountValue = it.discountValue,
                        finalPrice = it.finalPrice,
                    )
                },
                paymentDays = request.paymentDays,
            )

            val response = orchestrator.requestInvoiceSignatureFromVisit(
                visitId = request.visitId,
                request = signatureRequest,
                authContext = authContext
            )

            ok(response)
        } catch (e: InvoiceSignatureException) {
            logger.error("Error requesting invoice signature from visit", e)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error requesting invoice signature from visit", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }
    
    @GetMapping("/sessions/{sessionId}/status")
    fun getInvoiceSignatureStatus(
        @PathVariable sessionId: UUID,
        @RequestParam invoice_id: String
    ): ResponseEntity<InvoiceSignatureStatusResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val status = orchestrator.getSignatureStatus(sessionId, companyId, invoice_id)
            ok(status)
        } catch (e: Exception) {
            logger.error("Error getting invoice signature status", e)
            throw InvoiceSignatureException("Failed to get signature status", e)
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
            orchestrator.cancelSignatureSession(
                sessionId, companyId, invoiceId, userId.toString(), reason?.reason
            )

            ok(mapOf(
                "success" to true,
                "message" to "Invoice signature session cancelled successfully",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error cancelling invoice signature session", e)
            throw InvoiceSignatureException("Failed to cancel session", e)
        }
    }

    @PostMapping("/submit")
    fun submitInvoiceSignature(
        @Valid @RequestBody request: InvoiceSignatureSubmissionRequest
    ): ResponseEntity<InvoiceSignatureSubmissionResponse> {
        return try {
            val success = orchestrator.processSignatureSubmission(
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
            throw InvoiceSignatureException("Failed to submit invoice signature: ${e.message}", e)
        }
    }
}


// DTOs
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
    val timeoutMinutes: Int = 15,

    @JsonProperty("overriden_items")
    val overridenItems: List<CreateServiceCommand> = emptyList(),

    @JsonProperty("payment_days")
    val paymentDays: Long = 14,

    @JsonProperty("payment_method")
    val paymentMethod: String? = null
)

data class CancelInvoiceSignatureRequest(
    val reason: String? = null
)