// src/main/kotlin/com/carslab/crm/signature/service/ProtocolSignatureService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.visits.protocols.PdfService
import com.carslab.crm.domain.visits.protocols.SignatureData
import com.carslab.crm.modules.signature.api.controller.*
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.websocket.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class ProtocolSignatureService(
    private val pdfService: PdfService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val signatureCacheService: SignatureCacheService,
    private val objectMapper: ObjectMapper,
    private val protocolDocumentStorageService: ProtocolDocumentStorageService
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

            // 2. Utwórz sesję podpisu dokumentu
            val session = DocumentSignatureSession(
                sessionId = sessionId,
                documentId = UUID.randomUUID(),
                tabletId = request.tabletId,
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = "Podpis protokołu",
                instructions = request.instructions,
                businessContext = objectMapper.writeValueAsString(
                    mapOf(
                        "protocolId" to request.protocolId,
                        "documentType" to "PROTOCOL",
                        "originalPdfSize" to pdfData.size
                    )
                ),
                createdBy = userId,
                expiresAt = expiresAt
            )

            val savedSession = documentSignatureSessionRepository.save(session)

            // 3. Cache oryginalny dokument dla późniejszego użycia
            val originalDocumentCache = CachedSignatureData(
                sessionId = sessionId.toString(),
                signatureImageBase64 = "",
                signatureImageBytes = ByteArray(0), 
                originalDocumentBytes = pdfData,
                signedAt = Instant.now(),
                signerName = request.customerName,
                protocolId = request.protocolId,
                tabletId = request.tabletId,
                companyId = companyId,
                metadata = mapOf(
                    "documentType" to "PROTOCOL",
                    "instructions" to (request.instructions ?: ""),
                    "createdBy" to userId
                )
            )

            // Temporary cache z pustym podpisem
            signatureCacheService.cacheSignature(sessionId.toString(), originalDocumentCache)

            // 4. Wyślij żądanie podpisu do tableta
            val documentRequest = DocumentSignatureRequestDto(
                sessionId = sessionId.toString(),
                documentId = session.documentId.toString(),
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = "Protokół przyjęcia pojazdu",
                documentTitle = "Protokół przyjęcia pojazdu #${request.protocolId}",
                documentType = "PROTOCOL",
                pageCount = 1,
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

                // Powiadom frontend o rozpoczęciu procesu
                webSocketHandler.notifyFrontendSignatureStarted(
                    companyId,
                    sessionId.toString(),
                    request.protocolId
                )

                logger.info("Protocol signature request sent to tablet successfully: $sessionId")

                return ProtocolSignatureResponse(
                    success = true,
                    sessionId = sessionId,
                    message = "Protocol signature request sent to tablet successfully",
                    expiresAt = expiresAt,
                    protocolId = request.protocolId,
                    documentPreviewUrl = null
                )
            } else {
                documentSignatureSessionRepository.save(
                    savedSession.updateStatus(SignatureSessionStatus.ERROR, "Failed to send to tablet")
                )

                // Usuń z cache jeśli wysłanie nie powiodło się
                signatureCacheService.removeSignature(sessionId.toString())

                throw SignatureException("Failed to send signature request to tablet")
            }

        } catch (e: Exception) {
            logger.error("Error requesting protocol signature", e)
            throw SignatureException("Failed to request protocol signature: ${e.message}", e)
        }
    }

    /**
     * Przetwórz podpis otrzymany z tableta i zapisz w cache
     */
    fun processSignatureFromTablet(
        sessionId: String,
        signatureImageBase64: String
    ): Boolean {
        logger.info("Processing signature from tablet for session: $sessionId")

        try {
            // Dekoduj podpis
            val signatureBytes = Base64.getDecoder().decode(
                signatureImageBase64.substringAfter("base64,")
            )

            // Użyj updateSignature aby bezpiecznie zaktualizować cache
            val updatedData = signatureCacheService.updateSignature(sessionId) { cachedData ->
                cachedData.copy(
                    signatureImageBase64 = signatureImageBase64,
                    signatureImageBytes = signatureBytes,
                    signedAt = Instant.now()
                )
            }

            if (updatedData == null) {
                logger.error("Failed to update signature cache - session not found: $sessionId")
                return false
            }

            // Aktualizuj sesję w bazie
            val session = documentSignatureSessionRepository.findBySessionId(UUID.fromString(sessionId))
            if (session != null) {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.COMPLETED)
                )
            }

            // Powiadom frontend, że podpis jest gotowy
            val signatureCompletionData = SignatureCompletionData(
                sessionId = sessionId,
                success = true,
                signatureImageUrl = "/api/signatures/$sessionId/signature-image",
                signedDocumentUrl = "/api/signatures/$sessionId/signed-document",
                signedAt = updatedData.signedAt,
                signerName = updatedData.signerName,
                protocolId = updatedData.protocolId
            )

            webSocketHandler.notifyFrontendSignatureComplete(
                updatedData.companyId,
                sessionId,
                signatureCompletionData
            )

            logger.info("Signature processed and frontend notified for session: $sessionId")
            return true

        } catch (e: Exception) {
            logger.error("Error processing signature from tablet", e)
            return false
        }
    }

    /**
     * Pobierz podpisany dokument (integracja z frontendem)
     */
    fun getSignedDocument(sessionId: String, companyId: Long): ByteArray? {
        val cachedData = signatureCacheService.getSignature(sessionId)

        if (cachedData == null || cachedData.companyId != companyId) {
            logger.warn("Signed document not found or access denied for session: $sessionId")
            return null
        }

        if (cachedData.signatureImageBytes.isEmpty()) {
            logger.warn("Signature not yet available for session: $sessionId")
            return null
        }

        // Wywołaj PDFService.sign() z cached danymi
        return try {
            val signedPdf: ByteArray = pdfService.generatePdf(
                cachedData.protocolId!!,
                SignatureData(cachedData.signatureImageBytes),
            )

            // Opcjonalnie usuń z cache po użyciu
            signatureCacheService.removeSignature(sessionId)

            protocolDocumentStorageService.storeDocument(
                file = ByteArrayMultipartFile(
                    name = "protokol_odbioru_${cachedData.protocolId!!}.pdf",
                    originalFilename = "protokol_odbioru_${cachedData.protocolId!!}.pdf",
                    contentType = "application/pdf",
                    content = signedPdf
                ),
                protocolId = ProtocolId(cachedData.protocolId!!.toString()),
                documentType = "ACCEPTANCE_PROTOCOL",
                description = ""
            )

            logger.info("Signed document generated for session: $sessionId")
            signedPdf

        } catch (e: Exception) {
            logger.error("Error generating signed document for session: $sessionId", e)
            null
        }
    }

    /**
     * Pobierz obraz podpisu
     */
    fun getSignatureImage(sessionId: String, companyId: Long): ByteArray? {
        val cachedData = signatureCacheService.getSignature(sessionId)

        return if (cachedData?.companyId == companyId && cachedData.signatureImageBytes.isNotEmpty()) {
            cachedData.signatureImageBytes
        } else {
            null
        }
    }

    /**
     * Sprawdź status podpisu w cache
     */
    fun getSignatureStatus(sessionId: String, companyId: Long): Map<String, Any> {
        val cachedData = signatureCacheService.getSignature(sessionId)

        return if (cachedData?.companyId == companyId) {
            mapOf(
                "sessionId" to sessionId,
                "hasSignature" to cachedData.signatureImageBytes.isNotEmpty(),
                "signerName" to cachedData.signerName,
                "signedAt" to cachedData.signedAt,
                "protocolId" to (cachedData.protocolId ?: ""),
                "status" to if (cachedData.signatureImageBytes.isNotEmpty()) "READY" else "WAITING"
            )
        } else {
            mapOf(
                "sessionId" to sessionId,
                "status" to "NOT_FOUND"
            )
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
            )
        ) {
            documentSignatureSessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
            SignatureSessionStatus.EXPIRED
        } else {
            session.status
        }

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
            signedDocumentUrl = "/api/signatures/${sessionId}/signed-document",
            signatureImageUrl = "/api/signatures/${sessionId}/signature-image",
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

        // Usuń z cache
        signatureCacheService.removeSignature(sessionId.toString())

        // Powiadom tablet via WebSocket
        webSocketHandler.notifySessionCancellation(sessionId)

        logger.info("Protocol signature session cancelled: $sessionId by $userId")
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

    class ByteArrayMultipartFile(
        private val name: String,
        private val originalFilename: String,
        private val contentType: String,
        private val content: ByteArray
    ) : MultipartFile {

        override fun getName(): String = name

        override fun getOriginalFilename(): String = originalFilename

        override fun getContentType(): String = contentType

        override fun isEmpty(): Boolean = content.isEmpty()

        override fun getSize(): Long = content.size.toLong()

        override fun getBytes(): ByteArray = content

        override fun getInputStream(): InputStream = ByteArrayInputStream(content)

        override fun transferTo(dest: File) {
            dest.writeBytes(content)
        }
    }
}

class SignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)