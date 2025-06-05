// src/main/kotlin/com/carslab/crm/signature/service/SignatureService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.domain.service.FileStorageService
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.websocket.SignatureRequestDto
import com.carslab.crm.signature.websocket.VehicleInfoDto
import com.carslab.crm.signature.websocket.notifySessionCancellation
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
class SignatureService(
    private val signatureSessionRepository: SignatureSessionRepository,
    private val workstationRepository: WorkstationRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val fileStorageService: FileStorageService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(SignatureService::class.java)

    /**
     * Create signature session and send to tablet
     */
    fun createSignatureSession(
        companyId: Long,
        userId: String,
        tabletId: UUID,
        request: CreateSignatureSessionRequest
    ): UUID {
        logger.info("Creating signature session for tablet: $tabletId")

        // Check for existing active sessions for this workstation
        val activeSessions = signatureSessionRepository.findActiveSessionsByWorkstationId(request.workstationId)
        if (activeSessions.isNotEmpty()) {
            throw SignatureException("Workstation already has an active signature session")
        }

        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES) // 15 minutes timeout

        // Create signature session
        val session = SignatureSession(
            sessionId = sessionId,
            workstationId = request.workstationId,
            tabletId = tabletId,
            companyId = companyId,
            customerName = request.customerName,
            vehicleInfo = request.vehicleInfo?.let { objectMapper.writeValueAsString(it) },
            serviceType = request.serviceType,
            documentType = request.documentType,
            additionalNotes = request.additionalNotes,
            createdBy = userId,
            expiresAt = expiresAt
        )

        val savedSession = signatureSessionRepository.save(session)

        // Send to tablet via WebSocket
        val signatureRequest = SignatureRequestDto(
            sessionId = sessionId.toString(),
            companyId = companyId,
            workstationId = request.workstationId,
            customerName = request.customerName,
            vehicleInfo = VehicleInfoDto(
                make = request.vehicleInfo?.make ?: "",
                model = request.vehicleInfo?.model ?: "",
                licensePlate = request.vehicleInfo?.licensePlate ?: "",
                vin = request.vehicleInfo?.vin
            ),
            serviceType = request.serviceType ?: "Usługa serwisowa",
            documentType = request.documentType ?: "Potwierdzenie wykonania usługi"
        )

        val sent = webSocketHandler.sendSignatureRequest(tabletId, signatureRequest)

        if (sent) {
            // Update session status
            signatureSessionRepository.save(savedSession.updateStatus(SignatureStatus.SENT_TO_TABLET))

            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIGNATURE_REQUEST_SENT",
                performedBy = userId,
                tabletId = tabletId.toString(),
                auditContext = mapOf(
                    "customerName" to request.customerName,
                    "serviceType" to (request.serviceType ?: ""),
                    "workstationId" to request.workstationId
                )
            )
        } else {
            signatureSessionRepository.save(savedSession.updateStatus(
                SignatureStatus.ERROR,
                "Failed to send to tablet"
            ))
            throw SignatureException("Failed to send signature request to tablet")
        }

        logger.info("Signature session created: $sessionId")
        return sessionId
    }

    /**
     * Process signature submission from tablet
     */
    fun processSignatureSubmission(
        companyId: Long,
        submission: SignatureSubmission
    ): SignatureCompletionResponse {
        logger.info("Processing signature for session: ${submission.sessionId}")

        val sessionId = UUID.fromString(submission.sessionId)
        val session = signatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Signature session not found")

        if (!session.canBeSigned()) {
            throw SignatureException("Session cannot be signed (status: ${session.status}, expired: ${session.isExpired()})")
        }

        try {
            // Store signature image
            val signatureImagePath = fileStorageService.storeSignatureImage(
                sessionId, submission.signatureImage
            )

            // Update session
            val updatedSession = session.markAsSigned(signatureImagePath)
            signatureSessionRepository.save(updatedSession)

            // Generate URL for response
            val signatureImageUrl = fileStorageService.generateUrl(signatureImagePath)

            // Notify workstation about completion
            session.workstationId?.let { workstationId ->
                webSocketHandler.notifyWorkstation(workstationId, submission.sessionId, true, updatedSession.signedAt)
            }

            // Audit log
            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIGNATURE_COMPLETED",
                performedBy = session.customerName,
                deviceId = submission.deviceId.toString(),
                auditContext = mapOf(
                    "signatureSize" to submission.signatureImage.length,
                    "serviceType" to (session.serviceType ?: ""),
                    "workstationId" to (session.workstationId?.toString() ?: "")
                )
            )

            logger.info("Signature processed successfully: $sessionId")

            return SignatureCompletionResponse(
                success = true,
                sessionId = submission.sessionId,
                message = "Signature collected successfully",
                signedAt = updatedSession.signedAt,
                signatureImageUrl = signatureImageUrl
            )

        } catch (e: Exception) {
            logger.error("Error processing signature for session: $sessionId", e)

            // Update session with error
            signatureSessionRepository.save(session.updateStatus(
                SignatureStatus.ERROR,
                "Signature processing failed: ${e.message}"
            ))

            // Audit log error
            auditLog(
                sessionId = sessionId,
                companyId = companyId,
                action = "SIGNATURE_PROCESSING_FAILED",
                performedBy = session.customerName,
                deviceId = submission.deviceId.toString(),
                errorDetails = e.message
            )

            // Notify workstation about failure
            session.workstationId?.let { workstationId ->
                webSocketHandler.notifyWorkstation(workstationId, submission.sessionId, false, null)
            }

            throw SignatureException("Failed to process signature: ${e.message}", e)
        }
    }

    /**
     * Get signature session
     */
    @Transactional(readOnly = true)
    fun getSignatureSession(sessionId: UUID, companyId: Long): SignatureSessionDto? {
        val session = signatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        return SignatureSessionDto(
            sessionId = session.sessionId,
            workstationId = session.workstationId,
            tabletId = session.tabletId,
            companyId = session.companyId,
            customerName = session.customerName,
            vehicleInfo = session.vehicleInfo?.let { parseJson(it) as? VehicleInfoRequest },
            serviceType = session.serviceType,
            documentType = session.documentType,
            additionalNotes = session.additionalNotes,
            status = session.status,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            signedAt = session.signedAt,
            signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
            hasSignature = session.signatureImagePath != null
        )
    }

    /**
     * Get session status
     */
    @Transactional(readOnly = true)
    fun getSessionStatus(sessionId: UUID, companyId: Long): SignatureStatus {
        val session = signatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Session not found")

        // Check if session expired
        if (session.isExpired() && session.status in listOf(
                SignatureStatus.PENDING,
                SignatureStatus.SENT_TO_TABLET,
                SignatureStatus.IN_PROGRESS
            )) {
            // Update status to expired
            signatureSessionRepository.save(session.updateStatus(SignatureStatus.EXPIRED))
            return SignatureStatus.EXPIRED
        }

        return session.status
    }

    /**
     * Cancel signature session
     */
    fun cancelSignatureSession(
        sessionId: UUID,
        companyId: Long,
        userId: String,
        reason: String? = null
    ) {
        val session = signatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Session not found")

        if (session.status in listOf(SignatureStatus.COMPLETED, SignatureStatus.CANCELLED)) {
            throw SignatureException("Session cannot be cancelled (status: ${session.status})")
        }

        val cancelledSession = session.cancel(userId, reason)
        signatureSessionRepository.save(cancelledSession)

        // Notify tablet via WebSocket
        webSocketHandler.notifySessionCancellation(sessionId)

        // Notify workstation
        session.workstationId?.let { workstationId ->
            webSocketHandler.notifyWorkstation(workstationId, sessionId.toString(), false, null)
        }

        auditLog(
            sessionId = sessionId,
            companyId = companyId,
            action = "SESSION_CANCELLED",
            performedBy = userId,
            auditContext = mapOf(
                "reason" to (reason ?: "No reason provided"),
                "originalStatus" to session.status.name,
                "workstationId" to (session.workstationId?.toString() ?: "")
            )
        )

        logger.info("Signature session cancelled: $sessionId by $userId")
    }

    /**
     * Get signature image
     */
    @Transactional(readOnly = true)
    fun getSignatureImage(sessionId: UUID, companyId: Long): ByteArray? {
        val session = signatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        if (session.status != SignatureStatus.COMPLETED || session.signatureImagePath == null) {
            return null
        }

        auditLog(
            sessionId = sessionId,
            companyId = companyId,
            action = "SIGNATURE_DOWNLOADED",
            performedBy = "system" // Should be current user in real implementation
        )

        return fileStorageService.readFile(session.signatureImagePath!!)
    }

    /**
     * Get signature sessions with pagination
     */
    @Transactional(readOnly = true)
    fun getSignatureSessions(
        companyId: Long,
        page: Int,
        size: Int,
        status: SignatureStatus?
    ): List<SignatureSessionDto> {
        val pageable = PageRequest.of(page, size)

        val sessions = if (status != null) {
            signatureSessionRepository.findByCompanyIdAndStatus(companyId, status, pageable)
        } else {
            signatureSessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
        }

        return sessions.map { session ->
            SignatureSessionDto(
                sessionId = session.sessionId,
                workstationId = session.workstationId,
                tabletId = session.tabletId,
                companyId = session.companyId,
                customerName = session.customerName,
                vehicleInfo = session.vehicleInfo?.let { parseJson(it) as? VehicleInfoRequest },
                serviceType = session.serviceType,
                documentType = session.documentType,
                additionalNotes = session.additionalNotes,
                status = session.status,
                createdAt = session.createdAt,
                expiresAt = session.expiresAt,
                signedAt = session.signedAt,
                signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
                hasSignature = session.signatureImagePath != null
            )
        }
    }

    /**
     * Test tablet signature functionality
     */
    fun testTabletSignature(tabletId: UUID): Boolean {
        logger.info("Testing tablet signature: $tabletId")

        // Create test signature request
        val testRequest = SignatureRequestDto(
            sessionId = "test-${UUID.randomUUID()}",
            companyId = -1L, // Test company
            workstationId = UUID.randomUUID(),
            customerName = "Test Customer",
            vehicleInfo = VehicleInfoDto(
                make = "Test",
                model = "Vehicle",
                licensePlate = "TEST-123"
            ),
            serviceType = "Test serwisowy",
            documentType = "Test dokument"
        )

        return try {
            webSocketHandler.sendSignatureRequest(tabletId, testRequest)
        } catch (e: Exception) {
            logger.error("Error testing tablet $tabletId", e)
            false
        }
    }

    /**
     * Validate workstation access
     */
    @Transactional(readOnly = true)
    fun validateWorkstation(workstationId: UUID, companyId: Long): Workstation? {
        val workstation = workstationRepository.findById(workstationId).orElse(null)
        return if (workstation?.companyId == companyId) workstation else null
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
        // TODO: Implement audit logging
        logger.info("AUDIT: $action by $performedBy for session $sessionId")
    }
}

class SignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)