// Enhanced SignatureSessionManager with seller signature context
package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.finances.domain.signature.model.*
import com.carslab.crm.modules.finances.infrastructure.service.InvoiceSignatureCacheService
import com.carslab.crm.modules.finances.infrastructure.service.CachedInvoiceSignatureData
import com.carslab.crm.signature.infrastructure.persistance.entity.DocumentSignatureSession
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentSignatureSessionRepository
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Enhanced session manager that supports seller signature context
 */
@Service
@Transactional
class SignatureSessionManager(
    private val sessionRepository: DocumentSignatureSessionRepository,
    private val cacheService: InvoiceSignatureCacheService,
    private val objectMapper: ObjectMapper,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(SignatureSessionManager::class.java)

    fun createSession(request: SignatureSessionRequest): SignatureSession {
        logger.info("Creating signature session for invoice: ${request.invoiceId}")

        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        val session = DocumentSignatureSession(
            sessionId = sessionId,
            documentId = UUID.randomUUID(), // Document ID for signature system
            tabletId = request.tabletId,
            companyId = request.companyId,
            signerName = request.signerName,
            signatureTitle = request.signatureTitle,
            instructions = request.instructions,
            businessContext = objectMapper.writeValueAsString(
                mapOf(
                    "invoiceId" to request.invoiceId,
                    "documentType" to "INVOICE",
                    "replaceOriginalAttachment" to true,
                    "sellerId" to request.userId,
                    "includeSellerSignature" to true
                )
            ),
            createdBy = request.userId,
            expiresAt = expiresAt
        )

        val savedSession = sessionRepository.save(session)

        return SignatureSession(
            sessionId = savedSession.sessionId,
            invoiceId = request.invoiceId,
            tabletId = request.tabletId,
            companyId = request.companyId,
            signerName = request.signerName,
            expiresAt = expiresAt,
            status = SignatureSessionStatus.PENDING
        )
    }

    /**
     * Enhanced cache method that includes seller information
     */
    fun cacheDocumentForSignature(
        sessionId: UUID,
        document: UnifiedFinancialDocument,
        pdfBytes: ByteArray,
        signerName: String,
        sellerId: Long
    ) {
        logger.debug("Caching document for signature: ${document.id.value} with seller: $sellerId")
        val companyId = securityContext.getCurrentCompanyId()

        val cachedData = CachedInvoiceSignatureData(
            sessionId = sessionId.toString(),
            invoiceId = document.id.value,
            signatureImageBase64 = "",
            signatureImageBytes = ByteArray(0),
            originalInvoiceBytes = pdfBytes,
            signedAt = Instant.now(),
            signerName = signerName,
            tabletId = sessionId.toString(), // This should be proper tablet ID
            companyId = companyId,
            metadata = mapOf(
                "document" to document,
                "replaceOriginalAttachment" to true,
                "sellerId" to sellerId,
                "hasSellerSignature" to true
            )
        )

        cacheService.cacheInvoiceSignature(sessionId.toString(), cachedData)
        logger.debug("Cached signature data with seller context for session: $sessionId")
    }

    fun cleanupCache(sessionId: String) {
        cacheService.removeInvoiceSignature(sessionId)
        logger.info("Cleaned up cache for session: $sessionId")
    }

    fun updateCacheWithSignature(sessionId: String, signatureBase64: String, signatureBytes: ByteArray) {
        cacheService.updateInvoiceSignature(sessionId) { cachedData ->
            cachedData.copy(
                signatureImageBase64 = signatureBase64,
                signatureImageBytes = signatureBytes,
                signedAt = Instant.now()
            )
        }
        logger.debug("Updated cache with client signature for session: $sessionId")
    }

    fun markSessionAsSentToTablet(sessionId: UUID) {
        updateSessionStatus(sessionId, SignatureSessionStatus.SENT_TO_TABLET)
    }

    fun markSessionAsCompleted(sessionId: String) {
        updateSessionStatus(UUID.fromString(sessionId), SignatureSessionStatus.COMPLETED)
    }

    fun markSessionAsError(sessionId: String, errorMessage: String?) {
        updateSessionStatus(UUID.fromString(sessionId), SignatureSessionStatus.ERROR, errorMessage)
    }

    fun getSession(sessionId: String): SignatureSession? {
        val session = sessionRepository.findBySessionId(UUID.fromString(sessionId))
        return session?.let {
            SignatureSession(
                sessionId = it.sessionId,
                invoiceId = extractInvoiceIdFromContext(it.businessContext),
                tabletId = it.tabletId,
                companyId = it.companyId,
                signerName = it.signerName,
                expiresAt = it.expiresAt,
                status = it.status
            )
        }
    }

    fun getCachedData(sessionId: String): CachedSignatureData? {
        logger.debug("Getting cached data for session: $sessionId")

        val cachedData = cacheService.getInvoiceSignature(sessionId)
        if (cachedData == null) {
            logger.warn("No cached data found for session: $sessionId")
            return null
        }

        val sellerId = cachedData.metadata["sellerId"] as? Long ?: 1L
        logger.debug("Found cached data for session: $sessionId, signature bytes size: ${cachedData.signatureImageBytes.size}, seller: $sellerId")

        return CachedSignatureData(
            sessionId = cachedData.sessionId,
            document = cachedData.metadata["document"] as UnifiedFinancialDocument,
            originalPdfBytes = cachedData.originalInvoiceBytes,
            signatureImageBytes = cachedData.signatureImageBytes,
            signerName = cachedData.signerName,
            companyId = cachedData.companyId,
            sellerId = sellerId
        )
    }

    fun getSignatureStatus(sessionId: UUID, companyId: Long, invoiceId: String): InvoiceSignatureStatusResponse {
        logger.info("Getting signature status for invoice $invoiceId, session: $sessionId")

        val session = sessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw IllegalArgumentException("Signature session not found")

        val businessContext = session.businessContext?.let {
            objectMapper.readValue(it, Map::class.java)
        }
        val contextInvoiceId = businessContext?.get("invoiceId") as? String

        if (contextInvoiceId != invoiceId) {
            throw IllegalArgumentException("Session does not belong to this invoice")
        }

        val hasSellerSignature = businessContext?.get("includeSellerSignature") as? Boolean ?: false

        val currentStatus = if (session.isExpired() && session.status in listOf(
                SignatureSessionStatus.PENDING,
                SignatureSessionStatus.SENT_TO_TABLET,
                SignatureSessionStatus.VIEWING_DOCUMENT,
                SignatureSessionStatus.SIGNING_IN_PROGRESS
            )
        ) {
            sessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
            SignatureSessionStatus.EXPIRED
        } else {
            session.status
        }

        val statusMessage = if (hasSellerSignature) {
            "Invoice includes seller signature"
        } else {
            "Standard invoice signature process"
        }

        return InvoiceSignatureStatusResponse(
            success = true,
            sessionId = sessionId,
            invoiceId = invoiceId,
            status = mapToInvoiceStatus(currentStatus),
            signedAt = session.signedAt,
            signedInvoiceUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoice-signatures/sessions/$sessionId/signed-document?invoiceId=$invoiceId" else null,
            signatureImageUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoice-signatures/sessions/$sessionId/signature-image?invoiceId=$invoiceId" else null,
            timestamp = Instant.now(),
        )
    }

    fun cancelSession(sessionId: UUID, companyId: Long, invoiceId: String, userId: String, reason: String?) {
        val session = sessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw IllegalArgumentException("Session not found")

        val cancelledSession = session.cancel(userId, reason)
        sessionRepository.save(cancelledSession)

        // Cleanup cache
        cacheService.removeInvoiceSignature(sessionId.toString())
        logger.info("Cancelled session: $sessionId, reason: $reason")
    }

    private fun updateSessionStatus(sessionId: UUID, status: SignatureSessionStatus, errorMessage: String? = null) {
        val session = sessionRepository.findBySessionId(sessionId)
        if (session != null) {
            sessionRepository.save(session.updateStatus(status, errorMessage))
            logger.debug("Updated session $sessionId status to: $status")
        }
    }

    private fun extractInvoiceIdFromContext(businessContext: String?): String {
        return businessContext?.let {
            val context = objectMapper.readValue(it, Map::class.java)
            context["invoiceId"] as? String
        } ?: ""
    }

    private fun mapToInvoiceStatus(status: SignatureSessionStatus): InvoiceSignatureStatus {
        return when (status) {
            SignatureSessionStatus.PENDING -> InvoiceSignatureStatus.PENDING
            SignatureSessionStatus.SENT_TO_TABLET -> InvoiceSignatureStatus.SENT_TO_TABLET
            SignatureSessionStatus.VIEWING_DOCUMENT -> InvoiceSignatureStatus.VIEWING_INVOICE
            SignatureSessionStatus.SIGNING_IN_PROGRESS -> InvoiceSignatureStatus.SIGNING_IN_PROGRESS
            SignatureSessionStatus.COMPLETED -> InvoiceSignatureStatus.COMPLETED
            SignatureSessionStatus.EXPIRED -> InvoiceSignatureStatus.EXPIRED
            SignatureSessionStatus.CANCELLED -> InvoiceSignatureStatus.CANCELLED
            SignatureSessionStatus.ERROR -> InvoiceSignatureStatus.ERROR
        }
    }
}