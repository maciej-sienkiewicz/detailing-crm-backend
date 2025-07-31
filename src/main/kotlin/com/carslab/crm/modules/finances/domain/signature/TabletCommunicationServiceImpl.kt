package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.modules.finances.domain.signature.ports.TabletCommunicationService
import com.carslab.crm.modules.finances.domain.signature.model.SignatureSession
import com.carslab.crm.modules.finances.domain.signature.model.InvoiceSignatureException
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.service.WebSocketService
import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Handles communication with tablets
 */
@Service
class TabletCommunicationServiceImpl(
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketService: WebSocketService
) : TabletCommunicationService {

    private val logger = LoggerFactory.getLogger(TabletCommunicationServiceImpl::class.java)

    override fun validateTabletAccess(tabletId: UUID, companyId: Long) {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            ?: throw InvoiceSignatureException("Tablet not found: $tabletId")

        if (tablet.companyId != companyId) {
            throw InvoiceSignatureException("Tablet does not belong to company")
        }

        if (!webSocketService.isTabletConnected(tabletId)) {
            throw InvoiceSignatureException("Tablet is offline")
        }
    }

    override fun sendSignatureRequest(
        session: SignatureSession,
        document: UnifiedFinancialDocument,
        pdfBytes: ByteArray
    ): Boolean {
        logger.info("Sending signature request to tablet: ${session.tabletId}")

        val documentRequest = DocumentSignatureRequestDto(
            sessionId = session.sessionId.toString(),
            documentId = UUID.randomUUID().toString(), // Generate document ID for signature system
            companyId = session.companyId,
            signerName = session.signerName,
            signatureTitle = "Podpis na fakturze",
            documentTitle = "Faktura ${document.number}",
            documentType = "INVOICE",
            pageCount = 1,
            previewUrls = emptyList(),
            instructions = "Proszę podpisać fakturę",
            businessContext = mapOf(
                "invoiceId" to document.id.value,
                "documentType" to "INVOICE"
            ),
            timeoutMinutes = java.time.temporal.ChronoUnit.MINUTES.between(java.time.Instant.now(), session.expiresAt).toInt(),
            expiresAt = session.expiresAt.toString(),
            signatureFields = null
        )

        return try {
            webSocketService.sendDocumentSignatureRequestWithDocument(
                session.tabletId,
                documentRequest,
                pdfBytes
            )
        } catch (e: Exception) {
            logger.error("Failed to send signature request to tablet: ${session.tabletId}", e)
            false
        }
    }

    override fun notifySessionCancellation(sessionId: UUID) {
        webSocketService.notifySessionCancellation(sessionId)
    }
}
