// src/main/kotlin/com/carslab/crm/signature/service/DocumentSignatureService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.signature.api.dto.*
import com.carslab.crm.signature.domain.service.DocumentProcessingService
import com.carslab.crm.signature.domain.service.FileStorageService
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.websocket.broadcastToWorkstations
import com.carslab.crm.signature.websocket.notifySessionCancellation
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class DocumentSignatureService(
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val signatureDocumentRepository: SignatureDocumentRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val fileStorageService: FileStorageService,
    private val documentProcessingService: DocumentProcessingService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(DocumentSignatureService::class.java)

    /**
     * Przetwórz podpis dokumentu otrzymany z tableta
     */
    fun processDocumentSignatureSubmission(
        companyId: Long,
        submission: DocumentSignatureSubmission
    ): DocumentSignatureResponse {
        logger.info("Processing document signature for session: ${submission.sessionId}")

        val sessionId = UUID.fromString(submission.sessionId)
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Document signature session not found")

        if (!session.canBeSigned()) {
            throw SignatureException("Session cannot be signed (status: ${session.status}, expired: ${session.isExpired()})")
        }

        try {
            // 1. Zapisz obraz podpisu
            val signatureImagePath = fileStorageService.storeSignatureImage(
                sessionId, submission.signatureImage
            )

            // 2. Pobierz oryginalny dokument
            val document = signatureDocumentRepository.findById(session.documentId).orElse(null)
                ?: throw SignatureException("Original document not found")

            // 3. Zastosuj podpis do dokumentu PDF
            val signedDocumentPath = documentProcessingService.applySignatureToDocument(
                originalDocumentPath = document.filePath,
                signatureImagePath = signatureImagePath,
                signaturePlacement = submission.signaturePlacement,
                sessionId = sessionId
            )

            // 4. Zapisz placement podpisu jako JSON
            val signaturePlacementJson = submission.signaturePlacement?.let {
                objectMapper.writeValueAsString(it)
            }

            // 5. Aktualizuj sesję
            val updatedSession = session.markAsSigned(signatureImagePath, signaturePlacementJson)
                .copy(signedDocumentPath = signedDocumentPath)

            documentSignatureSessionRepository.save(updatedSession)

            // 6. Wygeneruj URLs dla odpowiedzi
            val signatureImageUrl = fileStorageService.generateUrl(signatureImagePath)
            val signedDocumentUrl = fileStorageService.generateUrl(signedDocumentPath)

            // 7. Powiadom CRM o zakończeniu przez WebSocket (jeśli to protokół)
            notifyCrmAboutCompletion(session, signatureImageUrl, signedDocumentUrl)

            logger.info("Document signature processed successfully: $sessionId")

            return DocumentSignatureResponse(
                success = true,
                sessionId = submission.sessionId,
                message = "Document signature collected successfully",
                signedAt = updatedSession.signedAt,
                signedDocumentUrl = signedDocumentUrl,
                signatureImageUrl = signatureImageUrl
            )

        } catch (e: Exception) {
            logger.error("Error processing document signature for session: $sessionId", e)

            // Aktualizuj sesję z błędem
            documentSignatureSessionRepository.save(session.updateStatus(
                SignatureSessionStatus.ERROR,
                "Signature processing failed: ${e.message}"
            ))

            throw SignatureException("Failed to process document signature: ${e.message}", e)
        }
    }

    /**
     * Powiadom CRM o zakończeniu podpisu (szczególnie dla protokołów)
     */
    private fun notifyCrmAboutCompletion(
        session: DocumentSignatureSession,
        signatureImageUrl: String,
        signedDocumentUrl: String
    ) {
        try {
            // Sprawdź czy to protokół z business context
            val businessContext = session.businessContext?.let {
                objectMapper.readValue(it, Map::class.java)
            }

            if (businessContext?.get("documentType") == "PROTOCOL") {
                val protocolId = (businessContext["protocolId"] as? Number)?.toLong()

                if (protocolId != null) {
                    // Powiadom wszystkie sesje CRM o zakończeniu podpisu protokołu
                    val notification = mapOf(
                        "type" to "protocol_signature_completed",
                        "payload" to mapOf(
                            "sessionId" to session.sessionId.toString(),
                            "protocolId" to protocolId,
                            "signatureImageUrl" to signatureImageUrl,
                            "signedDocumentUrl" to signedDocumentUrl,
                            "signedAt" to session.signedAt,
                            "signerName" to session.signerName,
                            "timestamp" to Instant.now()
                        )
                    )

                    // Wyślij notyfikację do wszystkich workstations w firmie
                    webSocketHandler.broadcastToWorkstations(session.companyId, notification)

                    logger.info("Notified CRM about protocol signature completion: $protocolId")
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to notify CRM about signature completion", e)
        }
    }

    /**
     * Pobierz sesję podpisu dokumentu
     */
    @Transactional(readOnly = true)
    fun getDocumentSignatureSession(sessionId: UUID, companyId: Long): DocumentSignatureSessionDto? {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        return DocumentSignatureSessionDto(
            sessionId = session.sessionId,
            documentId = session.documentId,
            tabletId = session.tabletId,
            companyId = session.companyId,
            signerName = session.signerName,
            signatureTitle = session.signatureTitle,
            instructions = session.instructions,
            businessContext = session.businessContext?.let {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(it, Map::class.java) as Map<String, Any>
            },
            status = session.status,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            signedAt = session.signedAt,
            signatureImageUrl = session.signatureImagePath?.let { fileStorageService.generateUrl(it) },
            signedDocumentUrl = session.signedDocumentPath?.let { fileStorageService.generateUrl(it) },
            hasSignature = session.signatureImagePath != null,
            signatureFields = session.signatureFields?.let {
                objectMapper.readValue(it, List::class.java) as List<SignatureFieldDefinition>
            },
            signaturePlacement = session.signaturePlacement?.let {
                objectMapper.readValue(it, SignaturePlacement::class.java)
            }
        )
    }

    /**
     * Sprawdź status sesji podpisu dokumentu
     */
    @Transactional(readOnly = true)
    fun getDocumentSignatureStatus(sessionId: UUID, companyId: Long): SignatureSessionStatus {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Session not found")

        // Sprawdź czy sesja wygasła
        if (session.isExpired() && session.status in listOf(
                SignatureSessionStatus.PENDING,
                SignatureSessionStatus.SENT_TO_TABLET,
                SignatureSessionStatus.VIEWING_DOCUMENT,
                SignatureSessionStatus.SIGNING_IN_PROGRESS
            )) {
            // Aktualizuj status na wygasły
            documentSignatureSessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
            return SignatureSessionStatus.EXPIRED
        }

        return session.status
    }

    /**
     * Anuluj sesję podpisu dokumentu
     */
    fun cancelDocumentSignatureSession(
        sessionId: UUID,
        companyId: Long,
        userId: String,
        reason: String? = null
    ) {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw SignatureException("Session not found")

        if (session.status in listOf(SignatureSessionStatus.COMPLETED, SignatureSessionStatus.CANCELLED)) {
            throw SignatureException("Session cannot be cancelled (status: ${session.status})")
        }

        val cancelledSession = session.cancel(userId, reason)
        documentSignatureSessionRepository.save(cancelledSession)

        // Powiadom tablet via WebSocket
        webSocketHandler.notifySessionCancellation(sessionId)

        logger.info("Document signature session cancelled: $sessionId by $userId")
    }

    /**
     * Pobierz podpisany dokument
     */
    @Transactional(readOnly = true)
    fun getSignedDocument(sessionId: UUID, companyId: Long): ByteArray? {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        if (session.status != SignatureSessionStatus.COMPLETED || session.signedDocumentPath == null) {
            return null
        }

        return fileStorageService.readFile(session.signedDocumentPath!!)
    }

    /**
     * Pobierz obraz podpisu
     */
    @Transactional(readOnly = true)
    fun getSignatureImage(sessionId: UUID, companyId: Long): ByteArray? {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        if (session.status != SignatureSessionStatus.COMPLETED || session.signatureImagePath == null) {
            return null
        }

        return fileStorageService.readFile(session.signatureImagePath!!)
    }
}