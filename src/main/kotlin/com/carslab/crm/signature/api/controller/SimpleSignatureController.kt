// src/main/kotlin/com/carslab/crm/signature/api/controller/SignatureController.kt
package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.service.SignatureService
import com.carslab.crm.signature.service.TabletManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/signatures")
class SignatureController(
    private val signatureService: SignatureService,
    private val tabletManagementService: TabletManagementService,
    private val securityContext: SecurityContext
) : BaseController() {

    /**
     * Create new signature session and send to tablet
     */
    @PostMapping("/request")
    fun createSignatureSession(
        @Valid @RequestBody request: CreateSignatureSessionRequest
    ): ResponseEntity<SignatureResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        logger.info("Creating signature session for workstation: ${request.workstationId}")

        return try {

            // Find available tablet for this workstation
            val tabletId = tabletManagementService
                .listingAllTablets(companyId)
                .filter { it.workstationId == request.workstationId.toString() }
                .firstOrNull()!!.id
                .let { UUID.fromString(it) }

            // Check if tablet is online
            if (!tabletManagementService.isTabletOnline(tabletId)) {
                return badRequest("Tablet is offline")
            }

            // Create signature session
            val sessionId = signatureService.createSignatureSession(
                companyId = companyId,
                userId = userId.toString(),
                tabletId = tabletId,
                request = request
            )

            ok(SignatureResponse(
                success = true,
                sessionId = sessionId,
                message = "Signature request sent to tablet successfully",
                expiresAt = Instant.now().plusSeconds(900) // 15 minutes
            ))

        } catch (e: Exception) {
            logger.error("Error creating signature session", e)
            throw SignatureException("Failed to create signature session: ${e.message}", e)
        }
    }

    /**
     * Submit signature from tablet
     */
    @PostMapping("/submit")
    fun submitSignature(
        @Valid @RequestBody submission: SignatureSubmission
    ): ResponseEntity<SignatureCompletionResponse> {
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Processing signature submission for session: ${submission.sessionId}")

        return try {
            val result = signatureService.processSignatureSubmission(
                companyId = companyId,
                submission = submission
            )

            ok(result)
        } catch (e: Exception) {
            logger.error("Error processing signature for session ${submission.sessionId}", e)

            ok(SignatureCompletionResponse(
                success = false,
                sessionId = submission.sessionId,
                message = "Failed to process signature: ${e.message}"
            ))
        }
    }

    /**
     * Get signature session details
     */
    @GetMapping("/sessions/{sessionId}")
    fun getSignatureSession(
        @PathVariable sessionId: UUID
    ): ResponseEntity<SignatureSessionDto> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val session = signatureService.getSignatureSession(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ok(session)
        } catch (e: Exception) {
            logger.error("Error retrieving signature session $sessionId", e)
            throw SignatureException("Failed to retrieve session", e)
        }
    }

    /**
     * Cancel signature session
     */
    @PostMapping("/sessions/{sessionId}/cancel")
    fun cancelSignatureSession(
        @PathVariable sessionId: UUID,
        @RequestBody(required = false) reason: CancelSessionRequest? = null
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        val userId = securityContext.getCurrentUserId()

        return try {
            signatureService.cancelSignatureSession(
                sessionId, companyId, userId.toString(), reason?.reason
            )

            ok(mapOf(
                "success" to true,
                "message" to "Signature session cancelled successfully",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error cancelling signature session $sessionId", e)
            throw SignatureException("Failed to cancel session", e)
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
            val status = signatureService.getSessionStatus(sessionId, companyId)
            ok(mapOf(
                "success" to true,
                "sessionId" to sessionId,
                "status" to status,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error getting session status $sessionId", e)
            throw SignatureException("Failed to get session status", e)
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
            val signatureData = signatureService.getSignatureImage(sessionId, companyId)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .header("Content-Disposition", "attachment; filename=\"signature-${sessionId}.png\"")
                .header("Content-Length", signatureData.size.toString())
                .body(signatureData)
        } catch (e: Exception) {
            logger.error("Error downloading signature for session $sessionId", e)
            throw SignatureException("Failed to download signature", e)
        }
    }

    /**
     * Test tablet connection
     */
    @PostMapping("/test/{tabletId}")
    fun testTabletConnection(
        @PathVariable tabletId: UUID
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        // Check access
        if (!tabletManagementService.checkTabletAccess(tabletId, companyId)) {
            return ResponseEntity.status(403).body(mapOf(
                "success" to false,
                "message" to "Access denied to tablet"
            ))
        }

        return try {
            val testResult = signatureService.testTabletSignature(tabletId)
            ok(mapOf(
                "success" to testResult,
                "message" to if (testResult) "Test signature sent to tablet" else "Failed to send test signature",
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error testing tablet $tabletId", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "Failed to test tablet: ${e.message}"
            ))
        }
    }

    /**
     * Get all signature sessions for company
     */
    @GetMapping("/sessions")
    fun getSignatureSessions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: SignatureStatus?
    ): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val sessions = signatureService.getSignatureSessions(companyId, page, size, status)

            ok(mapOf(
                "success" to true,
                "sessions" to sessions,
                "page" to page,
                "size" to size,
                "timestamp" to Instant.now()
            ))
        } catch (e: Exception) {
            logger.error("Error retrieving signature sessions", e)
            throw SignatureException("Failed to retrieve sessions", e)
        }
    }
}

class SignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)