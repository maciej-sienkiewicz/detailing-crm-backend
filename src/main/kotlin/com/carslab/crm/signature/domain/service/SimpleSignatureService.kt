package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.controller.SimpleSignatureException
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.domain.service.FileStorageService
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.websocket.notifySimpleSessionCancellation
import com.carslab.crm.signature.websocket.notifySimpleSignatureCompletion
import com.carslab.crm.signature.websocket.sendSimpleSignatureRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class SimpleSignatureService(
    private val simpleSessionRepository: SimpleSignatureSessionRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val fileStorageService: FileStorageService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(SimpleSignatureService::class.java)

    /**
     * Initiate simple signature request (no document involved)
     */
    fun initiateSimpleSignature(
        companyId: Long,
        userId: String,
        request: SimpleSignatureRequest
    ): UUID {
        logger.info("Initiating simple signature: ${request.signatureTitle} for tablet: ${request.tabletId}")

        // Check for existing active sessions for this tablet
        val activeSessions = simpleSessionRepository.findActiveSessionsByTabletId(request.tabletId)
        if (activeSessions.isNotEmpty()) {
            throw SimpleSignatureException("Tablet already has an active signature session")
        }

        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        // Create simple signature session
        val session = SimpleSignatureSession(
            sessionId = sessionId,
            tabletId = request.tabletId,
            companyId = companyId,
            signerName = request.signerName,
            signatureTitle = request.signatureTitle,
            instructions = request.instructions,
            businessContext = request.businessContext?.let { objectMapper.writeValueAsString(it) },
            signatureType = request.signatureType,
            externalReference = request.externalReference,
            createdBy = userId,
            expiresAt = expiresAt
        )

        val savedSession = simpleSessionRepository.save(session)

        // Send to tablet via WebSocket
        val signatureRequest = SimpleSignatureRequestDto(
            sessionId = sessionId.toString(),
            companyId = companyId,
            signerName = request.signerName,
            signatureTitle = request.signatureTitle,
            instructions = request.instructions,
            businessContext = request.businessContext,
            timeoutMinutes = request.timeoutMinutes,
            expiresAt = expiresAt,
            externalReference = request.externalReference,
            signatureType = request.signatureType
        )

        val sent = webSocketHandler.sendSimpleSignatureRequest(request.tabletId, signatureRequest)

        if (sent) {
            // Update session status
            simpleSessionRepository.save(savedSession.updateStatus(SimpleSignatureStatus.SENT_TO_TABLET))

            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIMPLE_SIGNATURE_REQUEST_SENT",
                performedBy = userId,
                tabletId = request.tabletId.toString(),
                auditContext = mapOf(
                    "signerName" to request.signerName,
                    "signatureType" to request.signatureType.name,
                    "timeoutMinutes" to request.timeoutMinutes
                )
            )
        } else {
            simpleSessionRepository.save(savedSession.updateStatus(
                SimpleSignatureStatus.ERROR,
                "Failed to send to tablet"
            ))
            throw SimpleSignatureException("Failed to send signature request to tablet")
        }

        logger.info("Simple signature session created: $sessionId")
        return sessionId
    }

    /**
     * Process simple signature submission from tablet
     */
    fun processSimpleSignature(
        companyId: Long,
        submission: SimpleSignatureSubmission
    ): SimpleSignatureCompletionResponse {
        logger.info("Processing simple signature for session: ${submission.sessionId}")

        val sessionId = UUID.fromString(submission.sessionId)
        val session = simpleSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SimpleSignatureException("Signature session not found")

        if (!session.canBeSigned()) {
            throw SimpleSignatureException("Session cannot be signed (status: ${session.status}, expired: ${session.isExpired()})")
        }

        try {
            // Store signature image
            val signatureImagePath = fileStorageService.storeSignatureImage(
                sessionId, submission.signatureImage
            )

            // Update session
            val updatedSession = session.markAsSigned(signatureImagePath)
            simpleSessionRepository.save(updatedSession)

            // Generate URL for response
            val signatureImageUrl = fileStorageService.generateUrl(signatureImagePath)

            // Audit log
            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIMPLE_SIGNATURE_COMPLETED",
                performedBy = session.signerName,
                deviceId = submission.deviceId.toString(),
                auditContext = mapOf(
                    "signatureType" to session.signatureType.name,
                    "signatureSize" to submission.signatureImage.length,
                    "externalReference" to (session.externalReference ?: "none")
                )
            )

            // Notify via WebSocket about completion
            webSocketHandler.notifySimpleSignatureCompletion(sessionId, true)

            logger.info("Simple signature processed successfully: $sessionId")

            return SimpleSignatureCompletionResponse(
                success = true,
                sessionId = submission.sessionId,
                message = "Signature collected successfully",
                signedAt = updatedSession.signedAt,
                signatureImageUrl = signatureImageUrl
            )

        } catch (e: Exception) {
            logger.error("Error processing simple signature for session: $sessionId", e)

            // Update session with error
            simpleSessionRepository.save(session.updateStatus(
                SimpleSignatureStatus.ERROR,
                "Signature processing failed: ${e.message}"
            ))

            // Audit log error
            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIMPLE_SIGNATURE_PROCESSING_FAILED",
                performedBy = session.signerName,
                deviceId = submission.deviceId.toString(),
                errorDetails = e.message
            )

            // Notify via WebSocket about failure
            webSocketHandler.notifySimpleSignatureCompletion(sessionId, false)

            throw SimpleSignatureException("Failed to process signature: ${e.message}", e)
        }
    }

    /**
     * Get simple signature session
     */
    @Transactional(readOnly = true)
    fun getSimpleSignatureSession(sessionId: UUID, companyId: Long): SimpleSignatureSessionDto? {
        val session = simpleSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        return SimpleSignatureSessionDto(
            sessionId = session.sessionId,
            tabletId = session.tabletId,
            companyId = session.companyId,
            signerName = session.signerName,
            signatureTitle = session.signatureTitle,
            instructions = session.instructions,
            businessContext = session.businessContext?.let { parseJson(it) as? Map<String, Any> },
            status = session.status,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            signedAt = session.signedAt,
            signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
            externalReference = session.externalReference,
            signatureType = session.signatureType
        )
    }

    /**
     * Get session status
     */
    @Transactional(readOnly = true)
    fun getSessionStatus(sessionId: UUID, companyId: Long): SimpleSignatureStatus {
        val session = simpleSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SimpleSignatureException("Session not found")

        // Check if session expired
        if (session.isExpired() && session.status in listOf(
                SimpleSignatureStatus.PENDING,
                SimpleSignatureStatus.SENT_TO_TABLET,
                SimpleSignatureStatus.IN_PROGRESS
            )) {
            // Update status to expired
            simpleSessionRepository.save(session.updateStatus(SimpleSignatureStatus.EXPIRED))
            return SimpleSignatureStatus.EXPIRED
        }

        return session.status
    }

    /**
     * Cancel simple signature session
     */
    fun cancelSimpleSignatureSession(
        sessionId: UUID,
        companyId: Long,
        userId: String,
        reason: String? = null
    ) {
        val session = simpleSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SimpleSignatureException("Session not found")

        if (session.status in listOf(SimpleSignatureStatus.COMPLETED, SimpleSignatureStatus.CANCELLED)) {
            throw SimpleSignatureException("Session cannot be cancelled (status: ${session.status})")
        }

        val cancelledSession = session.cancel(userId, reason)
        simpleSessionRepository.save(cancelledSession)

        // Notify tablet via WebSocket
        webSocketHandler.notifySimpleSessionCancellation(sessionId)

        auditLog(
            sessionId = sessionId,
            companyId = companyId,
            action = "SIMPLE_SESSION_CANCELLED",
            performedBy = userId,
            auditContext = mapOf(
                "reason" to (reason ?: "No reason provided"),
                "originalStatus" to session.status.name,
                "signatureType" to session.signatureType.name
            )
        )

        logger.info("Simple signature session cancelled: $sessionId by $userId")
    }

    /**
     * Get signature image
     */
    @Transactional(readOnly = true)
    fun getSignatureImage(sessionId: UUID, companyId: Long): ByteArray? {
        val session = simpleSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        if (session.status != SimpleSignatureStatus.COMPLETED || session.signatureImagePath == null) {
            return null
        }

        auditLog(
            sessionId = sessionId,
            companyId = companyId,
            action = "SIMPLE_SIGNATURE_DOWNLOADED",
            performedBy = "system" // Should be current user in real implementation
        )

        return fileStorageService.readFile("")
    }

    /**
     * Get simple signature sessions with pagination
     */
    @Transactional(readOnly = true)
    fun getSimpleSignatureSessions(
        companyId: Long,
        page: Int,
        size: Int,
        status: SimpleSignatureStatus?
    ): List<SimpleSignatureSessionDto> {
        val pageable = PageRequest.of(page, size)

        val sessions = if (status != null) {
            simpleSessionRepository.findByCompanyIdAndStatus(companyId, status, pageable)
        } else {
            simpleSessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
        }

        return sessions.map { session ->
            SimpleSignatureSessionDto(
                sessionId = session.sessionId,
                tabletId = session.tabletId,
                companyId = session.companyId,
                signerName = session.signerName,
                signatureTitle = session.signatureTitle,
                instructions = session.instructions,
                businessContext = session.businessContext?.let { parseJson(it) as? Map<String, Any> },
                status = session.status,
                createdAt = session.createdAt,
                expiresAt = session.expiresAt,
                signedAt = session.signedAt,
                signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
                externalReference = session.externalReference,
                signatureType = session.signatureType
            )
        }
    }

    // === PRIVATE HELPER METHODS ===

    private fun parseJson(json: String): Any? {
        return try {
            objectMapper.readValue(json, Any::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to parse JSON: $json", e)
            null
        }
    }

    private fun auditLog(
        sessionId: UUID,
        companyId: Long,
        action: String,
        performedBy: String,
        deviceId: String? = null,
        tabletId: String? = null,
        auditContext: Map<String, Any>? = null,
        errorDetails: String? = null
    ) {
        //  do nothing
    }
}