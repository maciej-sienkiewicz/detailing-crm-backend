package com.carslab.crm.signature.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.signature.domain.service.SignatureService
import com.carslab.crm.signature.api.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.util.*

@RestController
@RequestMapping("/api/signatures")
class SignatureController(
    private val signatureService: SignatureService
) : BaseController() {

    @PostMapping("/request")
    fun requestSignature(
        @Valid @RequestBody request: CreateSignatureSessionRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val tenantId = getTenantId(authentication)

        logger.info("Creating signature session for tenant: $tenantId, workstation: ${request.workstationId}")

        val session = signatureService.createSignatureSession(tenantId, request)
        val success = signatureService.requestSignature(tenantId, session.sessionId)

        if (success) {
            return ok(createSuccessResponse(
                "Signature request sent to tablet",
                mapOf(
                    "sessionId" to session.sessionId,
                    "expiresAt" to session.expiresAt
                )
            ))
        } else {
            return badRequest("No tablet available for signature request")
        }
    }

    @PostMapping
    fun submitSignature(
        @Valid @RequestBody submission: SignatureSubmission
    ): ResponseEntity<SignatureResponse> {
        logger.info("Submitting signature for session: ${submission.sessionId}")

        val response = signatureService.submitSignature(submission)

        return if (response.success) {
            logger.info("Signature submitted successfully for session: ${submission.sessionId}")
            created(response)
        } else {
            logger.warn("Signature submission failed for session: ${submission.sessionId} - ${response.message}")
            badRequest(response.message ?: "Signature submission failed")
        }
    }

    @GetMapping("/{sessionId}")
    fun getSignatureSession(@PathVariable sessionId: String): ResponseEntity<*> {
        val session = signatureService.getSignatureSession(sessionId)

        return if (session != null) {
            ok(mapOf(
                "sessionId" to session.sessionId,
                "status" to session.status,
                "customerName" to session.customerName,
                "signedAt" to session.signedAt,
                "hasSignature" to (session.signatureImage != null)
            ))
        } else {
            badRequest<Map<String, Any>>("Session not found")
        }
    }

    @GetMapping("/{sessionId}/image")
    fun getSignatureImage(@PathVariable sessionId: String): ResponseEntity<*> {
        val session = signatureService.getSignatureSession(sessionId)

        return if (session?.signatureImage != null) {
            ok(mapOf(
                "sessionId" to sessionId,
                "signatureImage" to session.signatureImage,
                "signedAt" to session.signedAt
            ))
        } else {
            badRequest<Map<String, Any>>("Signature not found")
        }
    }

    private fun getTenantId(authentication: Authentication): UUID {
        // Extract tenant ID from JWT or security context
        return authentication.details as? UUID
            ?: throw IllegalStateException("Tenant ID not found in authentication")
    }
}