package com.carslab.crm.modules.finances.domain

import com.carslab.crm.signature.events.DocumentSignatureCompletedEvent
import com.carslab.crm.modules.finances.domain.InvoiceSignatureService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class SignatureIntegrationService(
    private val invoiceSignatureService: InvoiceSignatureService
) {
    private val logger = LoggerFactory.getLogger(SignatureIntegrationService::class.java)

    @EventListener
    fun handleDocumentSignatureCompleted(event: DocumentSignatureCompletedEvent) {
        logger.info("Handling signature completed event for session: ${event.sessionId}")

        try {
            val session = findSessionBySessionId(event.sessionId)

            when (session?.documentType) {
                "INVOICE" -> {
                    logger.info("Processing invoice signature for session: ${event.sessionId}")
                    invoiceSignatureService.processSignatureFromTablet(event.sessionId, event.signatureImage)
                }
                "PROTOCOL" -> {
                    logger.info("Processing protocol signature for session: ${event.sessionId} - delegating to existing service")
                }
                else -> {
                    logger.warn("Unknown document type for session: ${event.sessionId}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling signature completed event for session: ${event.sessionId}", e)
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