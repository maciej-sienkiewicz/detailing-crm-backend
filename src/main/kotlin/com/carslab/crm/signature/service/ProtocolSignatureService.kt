// src/main/kotlin/com/carslab/crm/modules/signature/service/ProtocolSignatureService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.domain.visits.protocols.PdfService
import com.carslab.crm.modules.signature.api.controller.ProtocolSignatureRequest
import com.carslab.crm.modules.signature.api.controller.ProtocolSignatureResponse
import com.carslab.crm.modules.signature.api.controller.ProtocolSignatureStatus
import com.carslab.crm.modules.signature.api.controller.ProtocolSignatureStatusResponse
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.domain.service.FileStorageService
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.websocket.notifySessionCancellation
import com.carslab.crm.signature.websocket.sendDocumentSignatureRequest
import com.carslab.crm.signature.websocket.sendDocumentSignatureRequestWithDocument
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class ProtocolSignatureService(
    private val pdfService: PdfService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val signatureDocumentRepository: SignatureDocumentRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val fileStorageService: FileStorageService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(ProtocolSignatureService::class.java)

    fun requestProtocolSignature(
        companyId: Long,
        userId: String,
        request: ProtocolSignatureRequest
    ): ProtocolSignatureResponse {
        logger.info("Requesting protocol signature for protocol: ${request.protocolId}")

        val tablet = tabletDeviceRepository.findById(request.tabletId).orElse(null)
            ?: throw SignatureException("Tablet not found: ${request.tabletId}")

        if (tablet.companyId != companyId) {
            throw SignatureException("Tablet does not belong to company")
        }

        if (!webSocketHandler.isTabletConnected(request.tabletId)) {
            throw SignatureException("Tablet is offline")
        }

        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        try {
            // 1. Wygeneruj PDF z protokołu
            logger.info("Generating PDF for protocol: ${request.protocolId}")
            val pdfData = pdfService.generatePdf(request.protocolId)

            // 3. Utwórz sesję podpisu dokumentu
            val session = DocumentSignatureSession(
                sessionId = sessionId,
                documentId = UUID.randomUUID(),
                tabletId = request.tabletId,
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = "Podpis protokołu",
                instructions = request.instructions,
                businessContext = objectMapper.writeValueAsString(mapOf(
                    "protocolId" to request.protocolId,
                    "documentType" to "PROTOCOL"
                )),
                createdBy = userId,
                expiresAt = expiresAt
            )

            val savedSession = documentSignatureSessionRepository.save(session)

            // 5. Wyślij żądanie podpisu do tableta
            val documentRequest = DocumentSignatureRequestDto(
                sessionId = sessionId.toString(),
                documentId = "test",
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = "Protokół przyjęcia pojazdu",
                documentTitle = "Protokół przyjęcia pojazdu #${request.protocolId}",
                documentType = "PROTOCOL",
                pageCount = 1, // Protokoły to zwykle jedna strona
                previewUrls = emptyList(),
                instructions = request.instructions,
                businessContext = mapOf(
                    "protocolId" to request.protocolId,
                    "documentType" to "PROTOCOL"
                ),
                timeoutMinutes = request.timeoutMinutes,
                expiresAt = expiresAt.toString(),
                signatureFields = null
            )

            val sent = webSocketHandler.sendDocumentSignatureRequestWithDocument(
                request.tabletId,
                documentRequest,
                pdfData 
            )
            if (sent) {
                // Aktualizuj status sesji
                documentSignatureSessionRepository.save(
                    savedSession.updateStatus(SignatureSessionStatus.SENT_TO_TABLET)
                )

                logger.info("Protocol signature request sent to tablet successfully: $sessionId")

                return ProtocolSignatureResponse(
                    success = true,
                    sessionId = sessionId,
                    message = "Protocol signature request sent to tablet successfully",
                    expiresAt = expiresAt,
                    protocolId = request.protocolId,
                    documentPreviewUrl = null,
                )
            } else {
                documentSignatureSessionRepository.save(
                    savedSession.updateStatus(SignatureSessionStatus.ERROR, "Failed to send to tablet")
                )
                throw SignatureException("Failed to send signature request to tablet")
            }

        } catch (e: Exception) {
            logger.error("Error requesting protocol signature", e)
            throw SignatureException("Failed to request protocol signature: ${e.message}", e)
        }
    }
    
    fun getSignatureSessionStatus(sessionId: UUID, companyId: Long): ProtocolSignatureStatusResponse {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Signature session not found")

        val currentStatus = if (session.isExpired() && session.status in listOf(
                SignatureSessionStatus.PENDING,
                SignatureSessionStatus.SENT_TO_TABLET,
                SignatureSessionStatus.VIEWING_DOCUMENT,
                SignatureSessionStatus.SIGNING_IN_PROGRESS
            )) {
            documentSignatureSessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
            SignatureSessionStatus.EXPIRED
        } else {
            session.status
        }

        // Pobierz protocolId z business context
        val protocolId = try {
            val context = session.businessContext?.let { objectMapper.readValue(it, Map::class.java) }
            (context?.get("protocolId") as? Number)?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }

        return ProtocolSignatureStatusResponse(
            success = true,
            sessionId = sessionId,
            status = mapToProtocolStatus(currentStatus),
            protocolId = protocolId,
            signedAt = session.signedAt,
            signedDocumentUrl = session.signedDocumentPath?.let { fileStorageService.generateUrl(it) },
            signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
            timestamp = Instant.now()
        )
    }

    fun cancelSignatureSession(sessionId: UUID, companyId: Long, userId: String, reason: String?) {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Session not found")

        if (session.status in listOf(SignatureSessionStatus.COMPLETED, SignatureSessionStatus.CANCELLED)) {
            throw SignatureException("Session cannot be cancelled (status: ${session.status})")
        }

        val cancelledSession = session.cancel(userId, reason)
        documentSignatureSessionRepository.save(cancelledSession)

        // Powiadom tablet via WebSocket
        webSocketHandler.notifySessionCancellation(sessionId)

        logger.info("Protocol signature session cancelled: $sessionId by $userId")
    }

    fun getSignedProtocolDocument(sessionId: UUID, companyId: Long): ByteArray? {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        if (session.status != SignatureSessionStatus.COMPLETED || session.signedDocumentPath == null) {
            return null
        }

        return fileStorageService.readFile(session.signedDocumentPath!!)
    }

    private fun mapToProtocolStatus(status: SignatureSessionStatus): ProtocolSignatureStatus {
        return when (status) {
            SignatureSessionStatus.PENDING -> ProtocolSignatureStatus.PENDING
            SignatureSessionStatus.SENT_TO_TABLET -> ProtocolSignatureStatus.SENT_TO_TABLET
            SignatureSessionStatus.VIEWING_DOCUMENT -> ProtocolSignatureStatus.VIEWING_DOCUMENT
            SignatureSessionStatus.SIGNING_IN_PROGRESS -> ProtocolSignatureStatus.SIGNING_IN_PROGRESS
            SignatureSessionStatus.COMPLETED -> ProtocolSignatureStatus.COMPLETED
            SignatureSessionStatus.EXPIRED -> ProtocolSignatureStatus.EXPIRED
            SignatureSessionStatus.CANCELLED -> ProtocolSignatureStatus.CANCELLED
            SignatureSessionStatus.ERROR -> ProtocolSignatureStatus.ERROR
        }
    }
}

class SignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)