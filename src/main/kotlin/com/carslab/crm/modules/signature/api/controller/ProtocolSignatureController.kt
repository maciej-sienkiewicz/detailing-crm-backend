// src/main/kotlin/com/carslab/crm/modules/signature/api/controller/ProtocolSignatureController.kt
package com.carslab.crm.modules.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.shared.exception.TemplateNotFoundException
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.ProtocolSignatureService
import com.carslab.crm.signature.service.SignatureException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/protocol-signatures")
class ProtocolSignatureController(
    private val protocolSignatureService: ProtocolSignatureService,
    private val securityContext: SecurityContext
) : BaseController() {

    /**
     * Wyślij protokół do podpisu na tablet
     */
    @PostMapping("/request")
    fun requestProtocolSignature(
        @Valid @RequestBody request: ProtocolSignatureRequest
    ): ResponseEntity<ProtocolSignatureResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            val response = protocolSignatureService.requestProtocolSignature(
                companyId = companyId,
                userId = userId.toString(),
                request = request
            )

            ok(response)
        } catch (e: Exception) {
            logger.error("Error requesting protocol signature", e)
            throw SignatureException("Failed to request protocol signature: ${e.message}", e)
        }
    }

    /**
     * Pobierz status sesji podpisu protokołu
     */
    @GetMapping("/sessions/{sessionId}/status")
    fun getProtocolSignatureStatus(
        @PathVariable sessionId: UUID
    ): ResponseEntity<ProtocolSignatureStatusResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val status = protocolSignatureService.getSignatureSessionStatus(sessionId, companyId)
            if(status.status == ProtocolSignatureStatus.COMPLETED) {
                protocolSignatureService.getSignedDocument(sessionId.toString(), companyId)
            }
            ok(status)
        } catch (e: Exception) {
            logger.error("Error getting protocol signature status", e)
            throw SignatureException("Failed to get signature status", e)
        }
    }

    /**
     * Anuluj sesję podpisu protokołu
     */
    @PostMapping("/sessions/{sessionId}/cancel")
    fun cancelProtocolSignature(
        @PathVariable sessionId: UUID,
        @RequestBody(required = false) reason: CancelSessionRequest? = null
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            protocolSignatureService.cancelSignatureSession(
                sessionId, companyId, userId.toString(), reason?.reason
            )

            ok(mapOf(
                "success" to true,
                "message" to "Protocol signature session cancelled successfully",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error cancelling protocol signature session", e)
            throw SignatureException("Failed to cancel session", e)
        }
    }

    @ExceptionHandler(TemplateNotFoundException::class)
    fun handleTemplateNotFoundException(e: TemplateNotFoundException): ResponseEntity<Map<String, Any>> {
        logger.error("Template not found", e)

        val errorResponse: Map<String, Any> = mapOf(
            "success" to false,
            "error" to "TEMPLATE_NOT_FOUND",
            "message" to (e.message ?: "Template not found"),
            "timestamp" to Instant.now()
        )

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errorResponse)
    }
}

// DTOs dla protokołów
data class ProtocolSignatureRequest(
    @field:NotNull
    val protocolId: Long,

    @field:NotNull
    val tabletId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val customerName: String,

    @field:Size(max = 1000)
    val instructions: String? = "Proszę podpisać protokół przyjęcia pojazdu",

    @field:Positive
    @field:Max(30) // Max 30 minut
    val timeoutMinutes: Int = 15
)

data class ProtocolSignatureResponse(
    val success: Boolean,
    val sessionId: UUID,
    val message: String,
    val expiresAt: Instant,
    val protocolId: Long,
    val documentPreviewUrl: String? = null
)

data class ProtocolSignatureStatusResponse(
    val success: Boolean,
    val sessionId: UUID,
    val status: ProtocolSignatureStatus,
    val protocolId: Long,
    val signedAt: Instant? = null,
    val signedDocumentUrl: String? = null,
    val signatureImageUrl: String? = null,
    val timestamp: Instant
)

enum class ProtocolSignatureStatus {
    PENDING,
    GENERATING_PDF,
    SENT_TO_TABLET,
    VIEWING_DOCUMENT,
    SIGNING_IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    CANCELLED,
    ERROR
}