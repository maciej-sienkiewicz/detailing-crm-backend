package com.carslab.crm.signature.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.ratelimit.RateLimitingService
import com.carslab.crm.security.UserPrincipal
import com.carslab.crm.signature.api.dto.CreateSignatureSessionRequest
import com.carslab.crm.signature.api.dto.SignatureSubmission
import com.carslab.crm.signature.dto.*
import com.carslab.crm.signature.service.SignatureService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import java.util.*

@RestController
@RequestMapping("/api/signatures")
@Validated
class SignatureController(
    private val signatureService: SignatureService,
    private val rateLimitingService: RateLimitingService
) : BaseController() {

    @PostMapping("/request")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun requestSignature(
        @Valid @RequestBody request: CreateSignatureSessionRequest,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal
        val clientKey = "${userPrincipal.tenantId}:signature_request"

        if (!rateLimitingService.isAllowed(clientKey, 5)) {
            return ResponseEntity.status(429).body(
                createErrorResponse("Rate limit exceeded for signature requests")
            )
        }

        logger.info("Signature request from tenant: ${userPrincipal.tenantId}, user: ${userPrincipal.id}")

        return try {
            val session = signatureService.createSignatureSession(userPrincipal.tenantId, request)
            val success = signatureService.requestSignature(userPrincipal.tenantId, session.sessionId)

            if (success) {
                created(createSuccessResponse(
                    "Signature request sent to tablet",
                    mapOf(
                        "sessionId" to session.sessionId,
                        "expiresAt" to session.expiresAt
                    )
                ))
            } else {
                badRequest<Map<String, Any>>("No tablet available for signature request")
            }
        } catch (e: Exception) {
            logger.error("Error processing signature request", e)
            ResponseEntity.status(500).body(
                createErrorResponse("Internal server error processing signature request")
            )
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('TABLET') or hasRole('ADMIN')")
    fun submitSignature(
        @Valid @RequestBody submission: SignatureSubmission,
        authentication: Authentication
    ): ResponseEntity<SignatureResponse> {

        val userPrincipal = authentication.principal as UserPrincipal

        // For tablet submissions, verify device ID matches
        if (userPrincipal.authorities.any { it.authority == "ROLE_TABLET" }) {
            if (userPrincipal.id != submission.deviceId) {
                logger.warn("Device ID mismatch in signature submission: auth=${userPrincipal.id}, submission=${submission.deviceId}")
                return ResponseEntity.status(403).body(
                    SignatureResponse(
                        success = false,
                        sessionId = submission.sessionId,
                        message = "Device ID mismatch"
                    )
                )
            }
        }

        logger.info("Signature submission for session: ${submission.sessionId} from device: ${submission.deviceId}")

        return try {
            val response = signatureService.submitSignature(submission)
            if (response.success) {
                logger.info("Signature submitted successfully for session: ${submission.sessionId}")
                created(response)
            } else {
                logger.warn("Signature submission failed for session: ${submission.sessionId} - ${response.message}")
                badRequest(response.message ?: "Signature submission failed")
            }
        } catch (e: Exception) {
            logger.error("Unexpected error submitting signature for session: ${submission.sessionId}", e)
            ResponseEntity.status(500).body(
                SignatureResponse(
                    success = false,
                    sessionId = submission.sessionId,
                    message = "Internal server error"
                )
            )
        }
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getSignatureSession(
        @PathVariable @Pattern(regexp = "^[a-fA-F0-9-]{36}$") sessionId: String,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        return try {
            val session = signatureService.getSignatureSession(sessionId)

            if (session != null) {
                if (session.tenantId != userPrincipal.tenantId) {
                    logger.warn("Unauthorized access attempt to session $sessionId by tenant ${userPrincipal.tenantId}")
                    return ResponseEntity.status(403).body(createErrorResponse("Access denied"))
                }

                ok(mapOf(
                    "sessionId" to session.sessionId,
                    "status" to session.status,
                    "customerName" to session.customerName,
                    "signedAt" to session.signedAt,
                    "hasSignature" to (session.signatureImage != null),
                    "expiresAt" to session.expiresAt,
                    "createdAt" to session.createdAt
                ))
            } else {
                ResponseEntity.notFound().build<Map<String, Any>>()
            }
        } catch (e: Exception) {
            logger.error("Error retrieving signature session: $sessionId", e)
            ResponseEntity.status(500).body(createErrorResponse("Error retrieving session"))
        }
    }

    @GetMapping("/{sessionId}/image")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getSignatureImage(
        @PathVariable @Pattern(regexp = "^[a-fA-F0-9-]{36}$") sessionId: String,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        return try {
            val session = signatureService.getSignatureSession(sessionId)

            when {
                session == null -> ResponseEntity.notFound().build<Map<String, Any>>()
                session.tenantId != userPrincipal.tenantId -> {
                    logger.warn("Unauthorized access to signature image for session $sessionId by tenant ${userPrincipal.tenantId}")
                    ResponseEntity.status(403).body(createErrorResponse("Access denied"))
                }
                session.signatureImage == null -> ResponseEntity.notFound().build<Map<String, Any>>()
                else -> ok(mapOf(
                    "sessionId" to sessionId,
                    "signatureImage" to session.signatureImage,
                    "signedAt" to session.signedAt,
                    "customerName" to session.customerName
                ))
            }
        } catch (e: Exception) {
            logger.error("Error retrieving signature image for session: $sessionId", e)
            ResponseEntity.status(500).body(createErrorResponse("Error retrieving signature image"))
        }
    }

    private fun createErrorResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to java.time.Instant.now()
        )
    }
}