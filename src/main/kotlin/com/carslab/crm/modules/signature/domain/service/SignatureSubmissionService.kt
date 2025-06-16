// src/main/kotlin/com/carslab/crm/modules/signature/service/SignatureSubmissionService.kt
package com.carslab.crm.signature.service

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.visits.protocols.PdfService
import com.carslab.crm.domain.visits.protocols.SignatureData
import com.carslab.crm.modules.signature.api.controller.SignatureSubmissionRequest
import com.carslab.crm.signature.infrastructure.persistance.repository.*
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.signature.api.controller.SignatureResponse
import com.carslab.crm.modules.visits.infrastructure.storage.ProtocolDocumentStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*
import java.security.MessageDigest

@Service
@Transactional
class SignatureSubmissionService(
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val universalStorageService: UniversalStorageService,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Submit a regular signature from tablet
     */
    fun submitSignature(
        request: SignatureSubmissionRequest
    ): SignatureResponse {
        val sessionId = UUID.fromString(request.sessionId)

        logger.info("Processing signature submission for session: $sessionId")

        // Find and validate session
        val session = documentSignatureSessionRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("Signature session not found: $sessionId")

        if (!session.canBeSigned()) {
            throw IllegalArgumentException("Session cannot be signed. Status: ${session.status}")
        }

        // Save signature image to storage
        val signatureStorageId = "tu bedzie jakas logika..."
        logger.info("Signature image saved with storage ID: $signatureStorageId")

        // Update session as completed
        val updatedSession = session.markAsSigned(signatureStorageId)
        documentSignatureSessionRepository.save(updatedSession)

        logger.info("Signature submission completed successfully for session: $sessionId")

        return SignatureResponse(
            success = true,
            sessionId = request.sessionId,
            message = "Signature submitted successfully",
            signedAt = updatedSession.signedAt.toString(),
            signatureImageUrl = "tu bedzie jakas logika..."
        )
    }
    
    
    /**
     * Get signed document
     */
    @Transactional(readOnly = true)
    fun getSignedDocument(sessionId: UUID, companyId: Long): ByteArray? {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: return null

        return session.signedDocumentPath?.let { storageId ->
            try {
                universalStorageService.retrieveFile(storageId)
            } catch (e: Exception) {
                logger.error("Failed to read signed document for session $sessionId", e)
                null
            }
        }
    }

    /**
     * Get signature image
     */
    @Transactional(readOnly = true)
    fun getSignatureImage(sessionId: UUID, companyId: Long): ByteArray? {
        // Try regular signature session first
        val signatureSession = documentSignatureSessionRepository.findBySessionId(sessionId)
        if (signatureSession?.companyId == companyId && signatureSession.signatureImagePath != null) {
            return try {
                universalStorageService.retrieveFile(signatureSession.signatureImagePath!!)
            } catch (e: Exception) {
                logger.error("Failed to read signature image for session $sessionId", e)
                null
            }
        }

        // Try document signature session
        val documentSession = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
        if (documentSession?.signatureImagePath != null) {
            return try {
                universalStorageService.retrieveFile(documentSession.signatureImagePath!!)
            } catch (e: Exception) {
                logger.error("Failed to read document signature image for session $sessionId", e)
                null
            }
        }

        return null
    }

    private fun validateSignatureImage(signatureImage: String) {
        if (!signatureImage.startsWith("data:image/")) {
            throw IllegalArgumentException("Invalid signature image format")
        }

        val base64Data = signatureImage.substringAfter("base64,")
        if (base64Data.isBlank()) {
            throw IllegalArgumentException("Empty signature image data")
        }

        try {
            val decodedBytes = Base64.getDecoder().decode(base64Data)
            if (decodedBytes.size < 100) {
                throw IllegalArgumentException("Signature image too small - likely empty")
            }
            if (decodedBytes.size > 5_000_000) { // 5MB limit
                throw IllegalArgumentException("Signature image too large - maximum 5MB allowed")
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("Signature image") == true) throw e
            throw IllegalArgumentException("Invalid base64 signature image data")
        }
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