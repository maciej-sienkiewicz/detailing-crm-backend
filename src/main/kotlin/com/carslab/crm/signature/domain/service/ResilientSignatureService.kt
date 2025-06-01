package com.carslab.crm.signature.service

import com.carslab.crm.audit.service.AuditService
import com.carslab.crm.signature.api.dto.CreateSignatureSessionRequest
import com.carslab.crm.signature.api.dto.SignatureSubmission
import com.carslab.crm.signature.api.websocket.SignatureCompletedMessage
import com.carslab.crm.signature.api.websocket.SignatureRequestMessage
import com.carslab.crm.signature.api.websocket.VehicleInfoWS
import com.carslab.crm.signature.dto.*
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.exception.*
import com.carslab.crm.signature.infrastructure.persistance.repository.SignatureSessionRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
@Transactional
class ResilientSignatureService(
    private val signatureSessionRepository: SignatureSessionRepository,
    private val tabletManagementService: TabletManagementService,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val auditService: AuditService,
    private val metricsService: SignatureMetricsService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @CircuitBreaker(name = "signature-request", fallbackMethod = "fallbackCreateSession")
    @Retry(name = "signature-request")
    @TimeLimiter(name = "signature-request")
    fun createSignatureSessionWithResilience(
        tenantId: UUID,
        request: CreateSignatureSessionRequest
    ): CompletableFuture<SignatureSessionResponse> {

        return CompletableFuture.supplyAsync {
            try {
                metricsService.recordSignatureRequestAttempt(tenantId)

                val session = createSignatureSession(tenantId, request)
                val success = requestSignature(tenantId, session.sessionId)

                if (success) {
                    auditService.logSignatureRequest(tenantId, session.sessionId, "SUCCESS")
                    metricsService.recordSignatureRequestSuccess(tenantId)

                    SignatureSessionResponse(
                        success = true,
                        sessionId = session.sessionId,
                        expiresAt = session.expiresAt,
                        message = "Signature request sent successfully"
                    )
                } else {
                    auditService.logSignatureRequest(tenantId, session.sessionId, "NO_TABLET_AVAILABLE")
                    metricsService.recordSignatureRequestFailure(tenantId, "NO_TABLET")

                    SignatureSessionResponse(
                        success = false,
                        sessionId = session.sessionId,
                        message = "No tablet available for signature request"
                    )
                }
            } catch (e: Exception) {
                auditService.logSignatureRequest(tenantId, null, "ERROR", e.message)
                metricsService.recordSignatureRequestFailure(tenantId, "EXCEPTION")
                throw e
            }
        }
    }

    fun fallbackCreateSession(
        tenantId: UUID,
        request: CreateSignatureSessionRequest,
        ex: Exception
    ): CompletableFuture<SignatureSessionResponse> {

        logger.error("Signature request failed, using fallback", ex)
        auditService.logSignatureRequest(tenantId, null, "FALLBACK", ex.message)

        return CompletableFuture.completedFuture(
            SignatureSessionResponse(
                success = false,
                sessionId = null,
                message = "Service temporarily unavailable. Please try again later."
            )
        )
    }

    fun createSignatureSession(
        tenantId: UUID,
        request: CreateSignatureSessionRequest
    ): SignatureSession {
        val sessionId = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plus(2, ChronoUnit.MINUTES)

        val session = SignatureSession(
            sessionId = sessionId,
            tenantId = tenantId,
            workstationId = request.workstationId,
            customerId = request.customerId,
            customerName = request.customerName,
            vehicleMake = request.vehicleMake,
            vehicleModel = request.vehicleModel,
            licensePlate = request.licensePlate,
            vin = request.vin,
            serviceType = request.serviceType,
            documentId = request.documentId,
            documentType = request.documentType,
            expiresAt = expiresAt
        )

        return signatureSessionRepository.save(session)
    }

    fun requestSignature(tenantId: UUID, sessionId: String): Boolean {
        val session = signatureSessionRepository.findBySessionId(sessionId)
            ?: throw SignatureSessionNotFoundException(sessionId)

        if (session.tenantId != tenantId) {
            throw UnauthorizedTabletException("Session does not belong to tenant")
        }

        val tablet = tabletManagementService.selectTablet(session.workstationId)
            ?: throw TabletNotAvailableException()

        // Update session with tablet info
        val updatedSession = session.copy(
            tabletId = tablet.id,
            status = SignatureSessionStatus.SENT_TO_TABLET
        )
        signatureSessionRepository.save(updatedSession)

        // Send to tablet
        val message = SignatureRequestMessage(
            sessionId = session.sessionId,
            tenantId = session.tenantId,
            workstationId = session.workstationId,
            customerName = session.customerName,
            vehicleInfo = VehicleInfoWS(
                make = session.vehicleMake,
                model = session.vehicleModel,
                licensePlate = session.licensePlate,
                vin = session.vin
            ),
            serviceType = session.serviceType,
            documentType = session.documentType
        )

        return webSocketHandler.sendSignatureRequest(tablet.id, message)
    }

    @Transactional
    fun submitSignatureWithValidation(submission: SignatureSubmission): SignatureResponse {
        try {
            val session = signatureSessionRepository.findBySessionId(submission.sessionId)
                ?: throw SignatureSessionNotFoundException(submission.sessionId)

            // Validate session state
            validateSessionForSubmission(session, submission)

            // Validate signature image
            validateSignatureImage(submission.signatureImage)

            // Save signature
            val updatedSession = session.copy(
                signatureImage = submission.signatureImage,
                signedAt = submission.signedAt,
                status = SignatureSessionStatus.SIGNED
            )

            signatureSessionRepository.save(updatedSession)

            // Notify workstation
            notifyWorkstationOfCompletion(session, true)

            // Record metrics
            val duration = Duration.between(session.createdAt, submission.signedAt)
            metricsService.recordSignatureCompletion(session.tenantId, duration)

            auditService.logSignatureCompletion(
                session.tenantId,
                submission.sessionId,
                "SUCCESS"
            )

            return SignatureResponse(
                success = true,
                sessionId = submission.sessionId,
                signedAt = submission.signedAt,
                message = "Signature saved successfully"
            )

        } catch (e: Exception) {
            metricsService.recordSignatureSubmissionFailure(submission.sessionId, e::class.simpleName)
            auditService.logSignatureCompletion(null, submission.sessionId, "ERROR", e.message)
            throw e
        }
    }

    private fun notifyWorkstationOfCompletion(session: SignatureSession, success: Boolean) {
        val completedMessage = SignatureCompletedMessage(
            sessionId = session.sessionId,
            success = success,
            signedAt = session.signedAt
        )
        webSocketHandler.notifyWorkstation(session.workstationId, completedMessage)
    }

    private fun validateSessionForSubmission(session: SignatureSession, submission: SignatureSubmission) {
        when {
            session.status != SignatureSessionStatus.SENT_TO_TABLET ->
                throw InvalidSessionStateException("Session is not in valid state for signature submission")

            session.expiresAt.isBefore(Instant.now()) ->
                throw SessionExpiredException("Session has expired")

            session.tabletId != submission.deviceId ->
                throw UnauthorizedDeviceException("Signature submitted from unauthorized device")
        }
    }

    private fun validateSignatureImage(signatureImage: String) {
        // Validate base64 format
        if (!signatureImage.matches("^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$".toRegex())) {
            throw InvalidSignatureFormatException("Invalid signature image format")
        }

        // Check size (base64 encoded, so roughly 4/3 of actual size)
        if (signatureImage.length > 6_666_666) { // ~5MB limit
            throw SignatureImageTooLargeException("Signature image exceeds size limit")
        }

        // Basic content validation
        val base64Data = signatureImage.substringAfter("base64,")
        try {
            Base64.getDecoder().decode(base64Data)
        } catch (e: IllegalArgumentException) {
            throw InvalidSignatureFormatException("Invalid base64 encoding")
        }
    }

    fun getSignatureSession(sessionId: String): SignatureSession? {
        return signatureSessionRepository.findBySessionId(sessionId)
    }

    fun cancelSignatureSession(sessionId: String, tenantId: UUID): Boolean {
        val session = signatureSessionRepository.findBySessionId(sessionId)
            ?: throw SignatureSessionNotFoundException(sessionId)

        if (session.tenantId != tenantId) {
            throw UnauthorizedTabletException("Session does not belong to tenant")
        }

        if (session.status == SignatureSessionStatus.SIGNED) {
            return false // Cannot cancel already signed session
        }

        val updatedSession = session.copy(
            status = SignatureSessionStatus.CANCELLED
        )
        signatureSessionRepository.save(updatedSession)

        // Notify workstation about cancellation
        notifyWorkstationOfCompletion(session, false)

        return true
    }
}