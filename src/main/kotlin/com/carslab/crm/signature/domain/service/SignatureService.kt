package com.carslab.crm.signature.domain.service

import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.infrastructure.exception.*
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.api.websocket.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class SignatureService(
    private val signatureSessionRepository: SignatureSessionRepository,
    private val tabletManagementService: TabletManagementService,
    private val webSocketHandler: MultiTenantWebSocketHandler
) {

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
            status = SignatureSessionStatus.SENT_TO_TABLET,
            updatedAt = Instant.now()
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

    fun submitSignature(submission: SignatureSubmission): SignatureResponse {
        val session = signatureSessionRepository.findBySessionId(submission.sessionId)
            ?: throw SignatureSessionNotFoundException(submission.sessionId)

        if (session.status != SignatureSessionStatus.SENT_TO_TABLET) {
            return SignatureResponse(
                success = false,
                sessionId = submission.sessionId,
                message = "Invalid session status"
            )
        }

        if (session.expiresAt.isBefore(Instant.now())) {
            return SignatureResponse(
                success = false,
                sessionId = submission.sessionId,
                message = "Session expired"
            )
        }

        // Save signature
        val updatedSession = session.copy(
            signatureImage = submission.signatureImage,
            signedAt = submission.signedAt,
            status = SignatureSessionStatus.SIGNED,
            updatedAt = Instant.now()
        )
        signatureSessionRepository.save(updatedSession)

        // Notify workstation
        val completedMessage = SignatureCompletedMessage(
            sessionId = submission.sessionId,
            success = true,
            signedAt = submission.signedAt
        )
        webSocketHandler.notifyWorkstation(session.workstationId, completedMessage)

        return SignatureResponse(
            success = true,
            sessionId = submission.sessionId,
            signedAt = submission.signedAt
        )
    }

    fun getSignatureSession(sessionId: String): SignatureSession? {
        return signatureSessionRepository.findBySessionId(sessionId)
    }

    fun cleanupExpiredSessions() {
        val expiredSessions = signatureSessionRepository.findByExpiresAtBeforeAndStatus(
            Instant.now(),
            SignatureSessionStatus.SENT_TO_TABLET
        )

        expiredSessions.forEach { session ->
            val updatedSession = session.copy(
                status = SignatureSessionStatus.EXPIRED,
                updatedAt = Instant.now()
            )
            signatureSessionRepository.save(updatedSession)

            // Notify workstation about expiration
            val expiredMessage = SignatureCompletedMessage(
                sessionId = session.sessionId,
                success = false
            )
            webSocketHandler.notifyWorkstation(session.workstationId, expiredMessage)
        }
    }
}