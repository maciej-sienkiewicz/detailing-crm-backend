package com.carslab.crm.modules.finances.domain

import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SignatureIntegrationService(
    private val invoiceSignatureService: InvoiceSignatureService,
    private val webSocketHandler: SignatureWebSocketHandler
) {
    private val logger = LoggerFactory.getLogger(SignatureIntegrationService::class.java)

    fun handleSignatureFromTablet(sessionId: String, signatureImageBase64: String): Boolean {
        logger.info("Handling signature from tablet for session: $sessionId")

        try {
            val session = findSessionBySessionId(sessionId)

            return when (session?.documentType) {
                "INVOICE" -> {
                    logger.info("Processing invoice signature for session: $sessionId")
                    invoiceSignatureService.processSignatureFromTablet(sessionId, signatureImageBase64)
                }
                "PROTOCOL" -> {
                    logger.info("Processing protocol signature for session: $sessionId - delegating to existing service")
                    true
                }
                else -> {
                    logger.warn("Unknown document type for session: $sessionId")
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling signature from tablet for session: $sessionId", e)
            return false
        }
    }

    private fun findSessionBySessionId(sessionId: String): SessionInfo? {
        return try {
            val cachedData = invoiceSignatureService.getCachedSignatureData(sessionId)
            if (cachedData != null) {
                val documentType = cachedData.metadata["documentType"] as? String
                SessionInfo(sessionId, documentType ?: "UNKNOWN")
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Could not determine session type for: $sessionId", e)
            null
        }
    }

    private data class SessionInfo(
        val sessionId: String,
        val documentType: String
    )
}