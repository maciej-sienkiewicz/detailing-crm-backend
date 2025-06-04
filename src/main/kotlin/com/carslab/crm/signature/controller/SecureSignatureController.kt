package com.carslab.crm.signature.controller

import com.carslab.crm.ratelimit.RateLimitingService
import com.carslab.crm.security.UserPrincipal
import com.carslab.crm.security.TabletPrincipal
import com.carslab.crm.signature.api.dto.CreateSignatureSessionRequest
import com.carslab.crm.signature.api.dto.SignatureSubmission
import com.carslab.crm.signature.dto.SignatureResponse
import com.carslab.crm.signature.dto.SignatureSessionResponse
import com.carslab.crm.signature.service.ResilientSignatureService
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/signatures")
@Validated
class SecureSignatureController(
    private val resilientSignatureService: ResilientSignatureService,
    private val rateLimitingService: RateLimitingService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/request")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    @CircuitBreaker(name = "signature-request", fallbackMethod = "fallbackCreateSession")
    @Retry(name = "signature-request")
    @TimeLimiter(name = "signature-request")
    fun requestSignature(
        @Valid @RequestBody request: CreateSignatureSessionRequest,
        authentication: Authentication
    ): CompletableFuture<ResponseEntity<SignatureSessionResponse>> {

        val userPrincipal = authentication.principal as UserPrincipal
        val clientKey = "${userPrincipal.companyId}:signature_request"

        if (!rateLimitingService.isAllowed(clientKey, 5)) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(429).body(
                    SignatureSessionResponse(
                        success = false,
                        sessionId = null,
                        message = "Rate limit exceeded for signature requests"
                    )
                )
            )
        }

        logger.info("Signature request from company: ${userPrincipal.companyId}, user: ${userPrincipal.id}")

        return resilientSignatureService.createSignatureSessionWithResilience(userPrincipal.companyId, request)
            .thenApply { response ->
                if (response.success) {
                    ResponseEntity.status(201).body(response)
                } else {
                    ResponseEntity.badRequest().body(response)
                }
            }
            .exceptionally { throwable ->
                logger.error("Error processing signature request", throwable)
                ResponseEntity.status(500).body(
                    SignatureSessionResponse(
                        success = false,
                        sessionId = null,
                        message = "Internal server error processing signature request"
                    )
                )
            }
    }

    @PostMapping
    @PreAuthorize("hasRole('TABLET') or hasRole('ADMIN')")
    @CircuitBreaker(name = "signature-submit", fallbackMethod = "fallbackSubmitSignature")
    @Retry(name = "signature-submit")
    @TimeLimiter(name = "signature-submit")
    fun submitSignature(
        @Valid @RequestBody submission: SignatureSubmission,
        authentication: Authentication
    ): CompletableFuture<ResponseEntity<SignatureResponse>> {

        // Validate authentication first (synchronously)
        when (val principal = authentication.principal) {
            is TabletPrincipal -> {
                // Tablet submission - weryfikuj device ID
                if (principal.deviceId != submission.deviceId) {
                    logger.warn("Device ID mismatch in signature submission: auth=${principal.deviceId}, submission=${submission.deviceId}")
                    return CompletableFuture.completedFuture(
                        ResponseEntity.status(403).body(
                            SignatureResponse(
                                success = false,
                                sessionId = submission.sessionId,
                                message = "Device ID mismatch"
                            )
                        )
                    )
                }

                logger.info("Signature submission for session: ${submission.sessionId} from tablet: ${principal.deviceId}")
            }

            is UserPrincipal -> {
                // Admin/User submission
                logger.info("Signature submission for session: ${submission.sessionId} from user: ${principal.userUsername}")
            }

            else -> {
                logger.warn("Unknown principal type for signature submission: ${principal?.javaClass?.simpleName}")
                return CompletableFuture.completedFuture(
                    ResponseEntity.status(403).body(
                        SignatureResponse(
                            success = false,
                            sessionId = submission.sessionId,
                            message = "Invalid authentication type"
                        )
                    )
                )
            }
        }

        // Process submission asynchronously
        return resilientSignatureService.submitSignatureWithValidation(submission)
            .thenApply { response ->
                if (response.success) {
                    logger.info("Signature submitted successfully for session: ${submission.sessionId}")
                    ResponseEntity.status(201).body(response)
                } else {
                    logger.warn("Signature submission failed for session: ${submission.sessionId} - ${response.message}")
                    ResponseEntity.badRequest().body(response)
                }
            }
            .exceptionally { throwable ->
                logger.error("Unexpected error submitting signature for session: ${submission.sessionId}", throwable)
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
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getSignatureSession(
        @PathVariable @Pattern(regexp = "^[a-fA-F0-9-]{36}$") sessionId: String,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        return try {
            val session = resilientSignatureService.getSignatureSession(sessionId)

            if (session != null) {
                if (session.companyId != userPrincipal.companyId) {
                    logger.warn("Unauthorized access attempt to session $sessionId by company ${userPrincipal.companyId}")
                    return ResponseEntity.status(403).body(createErrorResponse("Access denied"))
                }

                ResponseEntity.ok(mapOf(
                    "sessionId" to session.sessionId,
                    "status" to session.status,
                    "customerName" to session.customerName,
                    "signedAt" to session.signedAt,
                    "hasSignature" to (session.signatureImage != null),
                    "expiresAt" to session.expiresAt,
                    "createdAt" to session.createdAt,
                    "vehicleInfo" to session.vehicleVin,
                    "serviceType" to session.serviceType,
                    "documentType" to session.documentType
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
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('MANAGER')")
    fun getSignatureImage(
        @PathVariable @Pattern(regexp = "^[a-fA-F0-9-]{36}$") sessionId: String,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        return try {
            val session = resilientSignatureService.getSignatureSession(sessionId)

            when {
                session == null -> ResponseEntity.notFound().build<Map<String, Any>>()

                session.companyId != userPrincipal.companyId -> {
                    logger.warn("Unauthorized access to signature image for session $sessionId by company ${userPrincipal.companyId}")
                    ResponseEntity.status(403).body(createErrorResponse("Access denied"))
                }

                session.signatureImage == null -> ResponseEntity.notFound().build<Map<String, Any>>()

                else -> ResponseEntity.ok(mapOf(
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

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun getCompanySignatures(
        @PathVariable companyId: Long,
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        // Sprawdź czy użytkownik ma dostęp do tej firmy
        if (userPrincipal.companyId != companyId && !userPrincipal.hasRole("ADMIN")) {
            return ResponseEntity.status(403).body(createErrorResponse("Access denied to company data"))
        }

        return try {
            val tenantId = convertCompanyIdToTenantId(companyId)
            val signatures = resilientSignatureService.getSignaturesByTenant(tenantId, page, size)

            ResponseEntity.ok(mapOf(
                "signatures" to signatures.map { session ->
                    mapOf(
                        "sessionId" to session.sessionId,
                        "customerName" to session.customerName,
                        "status" to session.status,
                        "createdAt" to session.createdAt,
                        "signedAt" to session.signedAt,
                        "vehicleInfo" to session.vehicleVin,
                        "serviceType" to session.serviceType
                    )
                },
                "page" to page,
                "size" to size,
                "companyId" to companyId,
                "total" to signatures.size
            ))
        } catch (e: Exception) {
            logger.error("Error retrieving signatures for company: $companyId", e)
            ResponseEntity.status(500).body(createErrorResponse("Error retrieving signatures"))
        }
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    fun cancelSignatureSession(
        @PathVariable @Pattern(regexp = "^[a-fA-F0-9-]{36}$") sessionId: String,
        authentication: Authentication
    ): ResponseEntity<*> {

        val userPrincipal = authentication.principal as UserPrincipal

        return try {
            val session = resilientSignatureService.getSignatureSession(sessionId)

            if (session == null) {
                return ResponseEntity.notFound().build<Map<String, Any>>()
            }

            if (session.companyId != userPrincipal.companyId) {
                return ResponseEntity.status(403).body(createErrorResponse("Access denied"))
            }

            // TODO: Implement cancel logic in ResilientSignatureService
            // resilientSignatureService.cancelSignatureSession(sessionId)

            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Signature session cancelled",
                "sessionId" to sessionId
            ))

        } catch (e: Exception) {
            logger.error("Error cancelling signature session: $sessionId", e)
            ResponseEntity.status(500).body(createErrorResponse("Error cancelling session"))
        }
    }

    // Fallback methods for Circuit Breaker
    fun fallbackCreateSession(
        request: CreateSignatureSessionRequest,
        authentication: Authentication,
        ex: Exception
    ): CompletableFuture<ResponseEntity<SignatureSessionResponse>> {
        logger.error("Circuit breaker activated for signature request", ex)
        return CompletableFuture.completedFuture(
            ResponseEntity.status(503).body(
                SignatureSessionResponse(
                    success = false,
                    sessionId = null,
                    message = "Service temporarily unavailable. Please try again later."
                )
            )
        )
    }

    fun fallbackSubmitSignature(
        submission: SignatureSubmission,
        authentication: Authentication,
        ex: Exception
    ): CompletableFuture<ResponseEntity<SignatureResponse>> {
        logger.error("Circuit breaker activated for signature submission", ex)
        return CompletableFuture.completedFuture(
            ResponseEntity.status(503).body(
                SignatureResponse(
                    success = false,
                    sessionId = submission.sessionId,
                    message = "Service temporarily unavailable. Please try again later."
                )
            )
        )
    }

    private fun createErrorResponse(message: String): Map<String, Any> {
        return mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to Instant.now()
        )
    }

    /**
     * Konwertuj companyId (Long) na tenantId (UUID) dla kompatybilności
     * W przyszłości można to zastąpić mapowaniem z bazy danych
     */
    private fun convertCompanyIdToTenantId(companyId: Long): UUID {
        // Temporary conversion - można zastąpić właściwym mapowaniem
        return UUID.fromString("${String.format("%08d", companyId)}-0000-0000-0000-000000000000")
    }
}