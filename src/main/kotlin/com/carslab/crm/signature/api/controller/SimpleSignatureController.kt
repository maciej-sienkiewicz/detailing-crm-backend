package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.SimpleSignatureService
import com.carslab.crm.signature.service.TabletManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/signature/simple")
class SimpleSignatureController(
    private val simpleSignatureService: SimpleSignatureService,
    private val tabletManagementService: TabletManagementService,
    private val securityContext: SecurityContext
) : BaseController() {

    /**
     * Request simple signature from tablet - NO DOCUMENT INVOLVED
     */
    @PostMapping("/request")
    fun requestSimpleSignature(
        @Valid @RequestBody request: SimpleSignatureRequest
    ): ResponseEntity<SimpleSignatureResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        logger.info("Requesting simple signature for tablet: ${request.tabletId}, signer: ${request.signerName}")

        // Validate tablet access and availability
        validateTabletAccess(request.tabletId, companyId)

        return try {
            val sessionId = simpleSignatureService.initiateSimpleSignature(
                companyId = companyId,
                userId = "ABC",
                request = request
            )

            ok(SimpleSignatureResponse(
                success = true,
                sessionId = sessionId,
                message = "Signature request sent to tablet successfully",
                expiresAt = Instant.now().plusSeconds(request.timeoutMinutes * 60L)
            ))
        } catch (e: Exception) {
            logger.error("Error requesting simple signature", e)
            throw SimpleSignatureException("Failed to request signature: ${e.message}", e)
        }
    }

    /**
     * Submit signature from tablet
     */
    @PostMapping("/submit")
    fun submitSimpleSignature(
        @Valid @RequestBody submission: SimpleSignatureSubmission
    ): ResponseEntity<SimpleSignatureCompletionResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Processing simple signature submission for session: ${submission.sessionId}")

        return try {
            val result = simpleSignatureService.processSimpleSignature(
                companyId = companyId,
                submission = submission
            )

            ok(result)
        } catch (e: Exception) {
            logger.error("Error processing simple signature for session ${submission.sessionId}", e)

            ok(SimpleSignatureCompletionResponse(
                success = false,
                sessionId = submission.sessionId,
                message = "Failed to process signature: ${e.message}"
            ))
        }
    }

    /**
     * Get simple signature session details
     */
    @GetMapping("/sessions/{sessionId}")
    fun getSimpleSignatureSession(
        @PathVariable sessionId: UUID
    ): ResponseEntity<SimpleSignatureSessionDto> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val session = simpleSignatureService.getSimpleSignatureSession(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ok(session)
        } catch (e: Exception) {
            logger.error("Error retrieving simple signature session $sessionId", e)
            throw SimpleSignatureException("Failed to retrieve session", e)
        }
    }

    /**
     * Cancel simple signature session
     */
    @PostMapping("/sessions/{sessionId}/cancel")
    fun cancelSimpleSignatureSession(
        @PathVariable sessionId: UUID,
        @RequestBody(required = false) reason: CancelSessionRequest? = null
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            simpleSignatureService.cancelSimpleSignatureSession(
                sessionId, companyId, "ABC", reason?.reason
            )

            ok(mapOf(
                "success" to true,
                "message" to "Simple signature session cancelled successfully",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error cancelling simple signature session $sessionId", e)
            throw SimpleSignatureException("Failed to cancel session", e)
        }
    }

    /**
     * Get session status
     */
    @GetMapping("/sessions/{sessionId}/status")
    fun getSessionStatus(
        @PathVariable sessionId: UUID
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val status = simpleSignatureService.getSessionStatus(sessionId, companyId)
            ok(mapOf(
                "success" to true,
                "sessionId" to sessionId,
                "status" to status,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error getting session status $sessionId", e)
            throw SimpleSignatureException("Failed to get session status", e)
        }
    }

    /**
     * Download signature image
     */
    @GetMapping("/sessions/{sessionId}/signature")
    fun downloadSignature(
        @PathVariable sessionId: UUID
    ): ResponseEntity<ByteArray> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val signatureData = simpleSignatureService.getSignatureImage(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .header("Content-Disposition", "attachment; filename=\"signature-${sessionId}.png\"")
                .header("Content-Length", signatureData.size.toString())
                .body(signatureData)
        } catch (e: Exception) {
            logger.error("Error downloading signature for session $sessionId", e)
            throw SimpleSignatureException("Failed to download signature", e)
        }
    }

    /**
     * Get all simple signature sessions for company (with pagination)
     */
    @GetMapping("/sessions")
    fun getSimpleSignatureSessions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: SimpleSignatureStatus?
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val sessions = simpleSignatureService.getSimpleSignatureSessions(
                companyId, page, size, status
            )

            ok(mapOf(
                "success" to true,
                "sessions" to sessions,
                "page" to page,
                "size" to size,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error retrieving simple signature sessions", e)
            throw SimpleSignatureException("Failed to retrieve sessions", e)
        }
    }

    private fun validateTabletAccess(tabletId: UUID, companyId: Long) {
        if (!tabletManagementService.checkTabletAccess(tabletId, companyId)) {
            throw SimpleSignatureException("Access denied to tablet $tabletId")
        }

        if (!tabletManagementService.isTabletOnline(tabletId)) {
            throw SimpleSignatureException("Tablet $tabletId is offline")
        }
    }
}

class SimpleSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)