package com.carslab.crm.signature.service

import com.carslab.crm.audit.service.AuditService
import com.carslab.crm.signature.api.dto.CreateSignatureSessionRequest
import com.carslab.crm.signature.api.dto.SignatureSubmission
import com.carslab.crm.signature.dto.SignatureResponse
import com.carslab.crm.signature.dto.SignatureSessionResponse
import com.carslab.crm.signature.entity.SignatureSession
import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureStatus
import com.carslab.crm.signature.infrastructure.persistance.repository.SignatureSessionRepository
import com.carslab.crm.signature.websocket.SignatureRequestDto
import com.carslab.crm.signature.websocket.VehicleInfoDto
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
@Transactional
class ResilientSignatureService(
    private val signatureSessionRepository: SignatureSessionRepository,
    private val tabletManagementService: TabletManagementService,
    private val webSocketService: WebSocketService,
    private val auditService: AuditService,
    private val metricsService: SignatureMetricsService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @CircuitBreaker(name = "signature-session-creation", fallbackMethod = "fallbackCreateSession")
    @Retry(name = "signature-session-creation")
    @TimeLimiter(name = "signature-session-creation")
    fun createSignatureSessionWithResilience(
        tenantId: Long,
        request: CreateSignatureSessionRequest
    ): CompletableFuture<SignatureSessionResponse> {

        return CompletableFuture.supplyAsync {
            val startTime = Instant.now()

            try {
                logger.info("Creating signature session for tenant: $tenantId, workstation: ${request.workstationId}")

                // Create session in database
                val session = createSignatureSession(tenantId, request)

                // Try to find and assign tablet
                val tablet = tabletManagementService.selectTabletForWorkstation(request.workstationId)

                if (tablet != null) {
                    logger.info("Selected tablet ${tablet.id} for session ${session.sessionId}")

                    // Create signature request for tablet
                    val signatureRequest = SignatureRequestDto(
                        sessionId = session.sessionId,
                        tenantId = tenantId,
                        workstationId = request.workstationId,
                        customerName = request.customerName,
                        vehicleInfo = VehicleInfoDto(
                            make = request.vehicleInfo?.make ?: "Unknown",
                            model = request.vehicleInfo?.model ?: "Unknown",
                            licensePlate = request.vehicleInfo?.licensePlate ?: "Unknown",
                            vin = request.vehicleInfo?.vin
                        ),
                        serviceType = request.serviceType ?: "General Service",
                        documentType = request.documentType ?: "Service Agreement"
                    )

                    // Send request to tablet via WebSocket
                    val sent = webSocketService.sendSignatureRequest(tablet.id, signatureRequest)

                    if (sent) {
                        auditService.logSignatureRequest(tenantId, session.sessionId, "SENT_TO_TABLET")
                        metricsService.incrementSignatureRequestsSent()
                        metricsService.recordSessionCreationTime(startTime)

                        logger.info("Signature request sent successfully to tablet ${tablet.id} for session ${session.sessionId}")

                        SignatureSessionResponse(
                            success = true,
                            sessionId = session.sessionId,
                            message = "Signature request sent to tablet",
                            tabletId = tablet.id,
                            workstationId = request.workstationId,
                            estimatedCompletionTime = session.expiresAt
                        )
                    } else {
                        logger.warn("Failed to send signature request to tablet ${tablet.id} for session ${session.sessionId}")
                        auditService.logSignatureRequest(tenantId, session.sessionId, "FAILED_TO_SEND")
                        metricsService.incrementSignatureRequestsFailed()

                        SignatureSessionResponse(
                            success = false,
                            sessionId = session.sessionId,
                            message = "Failed to send request to tablet - tablet may be offline",
                            tabletId = tablet.id,
                            workstationId = request.workstationId
                        )
                    }
                } else {
                    logger.warn("No tablet available for workstation ${request.workstationId}, session ${session.sessionId}")
                    auditService.logSignatureRequest(tenantId, session.sessionId, "NO_TABLET_AVAILABLE")
                    metricsService.incrementSignatureRequestsFailed()

                    SignatureSessionResponse(
                        success = false,
                        sessionId = session.sessionId,
                        message = "No tablet available for signature request. Please ensure a tablet is connected to your workstation.",
                        workstationId = request.workstationId
                    )
                }

            } catch (e: Exception) {
                logger.error("Error creating signature session with resilience for tenant $tenantId", e)
                auditService.logSignatureRequest(tenantId, "unknown", "CREATION_FAILED")
                metricsService.incrementSignatureRequestsFailed()

                SignatureSessionResponse(
                    success = false,
                    sessionId = null,
                    message = "Failed to create signature session: ${e.message}"
                )
            }
        }
    }

    @CircuitBreaker(name = "signature-submission", fallbackMethod = "fallbackSubmitSignature")
    @Retry(name = "signature-submission")
    @TimeLimiter(name = "signature-submission")
    fun submitSignatureWithValidation(submission: SignatureSubmission): CompletableFuture<SignatureResponse> {

        return CompletableFuture.supplyAsync {
            val startTime = Instant.now()

            try {
                logger.info("Processing signature submission for session: ${submission.sessionId} from device: ${submission.deviceId}")

                // Find session
                val session = signatureSessionRepository.findBySessionId(submission.sessionId)
                if (session == null) {
                    logger.warn("Session not found: ${submission.sessionId}")
                    return@supplyAsync SignatureResponse(
                        success = false,
                        sessionId = submission.sessionId,
                        message = "Session not found"
                    )
                }

                // Validate session status
                if (session.status != SignatureStatus.PENDING) {
                    logger.warn("Session ${submission.sessionId} is not in pending status: ${session.status}")
                    return@supplyAsync SignatureResponse(
                        success = false,
                        sessionId = submission.sessionId,
                        message = "Session is not in pending status (current: ${session.status})"
                    )
                }

                // Check if session expired
                if (session.expiresAt.isBefore(Instant.now())) {
                    logger.warn("Session ${submission.sessionId} has expired")
                    session.status = SignatureStatus.EXPIRED
                    signatureSessionRepository.save(session)

                    metricsService.incrementSignaturesExpired()

                    return@supplyAsync SignatureResponse(
                        success = false,
                        sessionId = submission.sessionId,
                        message = "Session has expired"
                    )
                }

                // Validate signature data
                if (submission.signatureImage.isBlank()) {
                    logger.warn("Empty signature data for session ${submission.sessionId}")
                    return@supplyAsync SignatureResponse(
                        success = false,
                        sessionId = submission.sessionId,
                        message = "Signature data cannot be empty"
                    )
                }

                // Process and save signature
                session.signatureImage = submission.signatureImage
                session.signedAt = submission.signedAt
                session.status = SignatureStatus.COMPLETED

                val updatedSession = signatureSessionRepository.save(session)
                logger.info("Signature saved successfully for session ${submission.sessionId}")

                // Notify workstation of completion
                notifyWorkstationOfCompletion(updatedSession, true, submission.signedAt)

                // Audit and metrics
                auditService.logSignatureRequest(session.tenantId, session.sessionId, "COMPLETED")
                metricsService.incrementSignaturesCompleted()
                metricsService.recordSignatureProcessingTime(startTime)

                logger.info("Signature successfully processed for session: ${submission.sessionId}")

                SignatureResponse(
                    success = true,
                    sessionId = submission.sessionId,
                    message = "Signature processed successfully",
                    signedAt = submission.signedAt
                )

            } catch (e: Exception) {
                logger.error("Error processing signature submission for session: ${submission.sessionId}", e)
                auditService.logSignatureRequest(UUID.randomUUID(), submission.sessionId, "SUBMISSION_FAILED")
                metricsService.incrementSignatureSubmissionsFailed()

                SignatureResponse(
                    success = false,
                    sessionId = submission.sessionId,
                    message = "Failed to process signature: ${e.message}"
                )
            }
        }
    }

    fun getSignatureSession(sessionId: String): SignatureSession? {
        return try {
            logger.debug("Retrieving signature session: $sessionId")
            signatureSessionRepository.findBySessionId(sessionId)
        } catch (e: Exception) {
            logger.error("Error retrieving signature session: $sessionId", e)
            null
        }
    }

    fun getSignaturesByTenant(tenantId: UUID, page: Int = 0, size: Int = 20): List<SignatureSession> {
        return try {
            logger.debug("Retrieving signatures for tenant: $tenantId, page: $page, size: $size")

            // Simple pagination - can be improved with Spring Data Pageable
            val allSessions = signatureSessionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            val startIndex = page * size
            val endIndex = minOf(startIndex + size, allSessions.size)

            if (startIndex >= allSessions.size) {
                emptyList()
            } else {
                allSessions.subList(startIndex, endIndex)
            }
        } catch (e: Exception) {
            logger.error("Error retrieving signatures for tenant: $tenantId", e)
            emptyList()
        }
    }

    @Transactional
    fun cancelSignatureSession(sessionId: String): Boolean {
        return try {
            val session = signatureSessionRepository.findBySessionId(sessionId)

            if (session == null) {
                logger.warn("Cannot cancel - session not found: $sessionId")
                return false
            }

            if (session.status != SignatureStatus.PENDING) {
                logger.warn("Cannot cancel - session $sessionId is not pending (status: ${session.status})")
                return false
            }

            session.status = SignatureStatus.CANCELLED
            signatureSessionRepository.save(session)

            // Notify workstation
            notifyWorkstationOfCompletion(session, false, null)

            // Audit
            auditService.logSignatureRequest(session.tenantId, sessionId, "CANCELLED")

            logger.info("Signature session cancelled: $sessionId")
            true

        } catch (e: Exception) {
            logger.error("Error cancelling signature session: $sessionId", e)
            false
        }
    }

    // Scheduled task to expire old sessions
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    fun expireOldSessions() {
        try {
            val now = Instant.now()
            val expiredSessions = signatureSessionRepository.findByStatusAndExpiresAtBefore(
                SignatureStatus.PENDING,
                now
            )

            if (expiredSessions.isNotEmpty()) {
                logger.info("Found ${expiredSessions.size} expired sessions to process")

                expiredSessions.forEach { session ->
                    try {
                        session.status = SignatureStatus.EXPIRED
                        signatureSessionRepository.save(session)

                        // Notify workstation about expiration
                        notifyWorkstationOfCompletion(session, false, null)

                        auditService.logSignatureRequest(session.tenantId, session.sessionId, "EXPIRED")
                        metricsService.incrementSignaturesExpired()

                        logger.debug("Expired session: ${session.sessionId}")
                    } catch (e: Exception) {
                        logger.error("Error expiring session ${session.sessionId}", e)
                    }
                }

                logger.info("Expired ${expiredSessions.size} signature sessions")
            }

        } catch (e: Exception) {
            logger.error("Error during scheduled session expiration", e)
        }
    }

    private fun createSignatureSession(tenantId: Long, request: CreateSignatureSessionRequest): SignatureSession {
        val sessionId = UUID.randomUUID().toString()
        val now = Instant.now()

        val session = SignatureSession(
            sessionId = sessionId,
            companyId = tenantId,
            workstationId = request.workstationId,
            customerName = request.customerName,
            serviceType = request.serviceType,
            documentType = request.documentType,
            status = SignatureStatus.PENDING,
            expiresAt = now.plus(30, ChronoUnit.MINUTES), // 30 minutes expiration
            signatureImage = null,
            signedAt = null
        )

        val savedSession = signatureSessionRepository.save(session)
        logger.info("Created signature session: ${savedSession.sessionId} for tenant: $tenantId")

        return savedSession
    }

    private fun notifyWorkstationOfCompletion(session: SignatureSession, success: Boolean, signedAt: Instant? = null) {
        try {
            webSocketService.notifyWorkstation(
                workstationId = session.workstationId,
                sessionId = session.sessionId,
                success = success,
                signedAt = signedAt
            )
            logger.debug("Notified workstation ${session.workstationId} about session ${session.sessionId} completion (success: $success)")
        } catch (e: Exception) {
            logger.error("Failed to notify workstation ${session.workstationId} about session ${session.sessionId}", e)
        }
    }

    // Fallback methods for Circuit Breaker
    fun fallbackCreateSession(
        tenantId: UUID,
        request: CreateSignatureSessionRequest,
        ex: Exception
    ): CompletableFuture<SignatureSessionResponse> {
        logger.error("Fallback: Failed to create signature session for tenant: $tenantId", ex)

        return CompletableFuture.completedFuture(
            SignatureSessionResponse(
                success = false,
                sessionId = null,
                message = "Service temporarily unavailable due to high load. Please try again in a few moments."
            )
        )
    }

    fun fallbackSubmitSignature(
        submission: SignatureSubmission,
        ex: Exception
    ): CompletableFuture<SignatureResponse> {
        logger.error("Fallback: Failed to submit signature for session: ${submission.sessionId}", ex)

        return CompletableFuture.completedFuture(
            SignatureResponse(
                success = false,
                sessionId = submission.sessionId,
                message = "Service temporarily unavailable due to high load. Your signature may have been saved - please check the session status."
            )
        )
    }
}