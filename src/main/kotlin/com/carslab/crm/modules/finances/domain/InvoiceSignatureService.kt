package com.carslab.crm.modules.finances.domain

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.modules.invoice_templates.domain.InvoiceTemplateService
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.invoice_templates.domain.ports.InvoiceTemplateRepository
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.signature.infrastructure.persistance.entity.DocumentSignatureSession
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentSignatureSessionRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.websocket.broadcastToWorkstations
import com.carslab.crm.signature.service.SignatureCacheService
import com.carslab.crm.signature.service.CachedSignatureData
import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.signature.websocket.notifySessionCancellation
import com.carslab.crm.signature.websocket.sendDocumentSignatureRequestWithDocument
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class InvoiceSignatureService(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val invoiceTemplateService: InvoiceTemplateService,
    private val templateRepository: InvoiceTemplateRepository,
    private val companySettingsService: CompanySettingsDomainService,
    private val logoStorageService: LogoStorageService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketHandler: SignatureWebSocketHandler,
    private val signatureCacheService: SignatureCacheService,
    private val universalStorageService: UniversalStorageService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(InvoiceSignatureService::class.java)

    fun requestInvoiceSignature(
        companyId: Long,
        userId: String,
        invoiceId: String,
        request: InvoiceSignatureRequest
    ): InvoiceSignatureResponse {
        logger.info("Requesting invoice signature for invoice: {} by user: {}", invoiceId, userId)

        val tablet = tabletDeviceRepository.findById(request.tabletId).orElse(null)
            ?: throw InvoiceSignatureException("Tablet not found: ${request.tabletId}")

        if (tablet.companyId != companyId) {
            throw InvoiceSignatureException("Tablet does not belong to company")
        }

        if (!webSocketHandler.isTabletConnected(request.tabletId)) {
            throw InvoiceSignatureException("Tablet is offline")
        }

        val document = unifiedDocumentService.getDocumentById(invoiceId)
        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        try {
            val invoicePdfWithoutSignature = generateInvoicePdfWithoutSignature(document, companyId)

            val session = DocumentSignatureSession(
                sessionId = sessionId,
                documentId = UUID.randomUUID(),
                tabletId = request.tabletId,
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = request.signatureTitle,
                instructions = request.instructions,
                businessContext = objectMapper.writeValueAsString(
                    mapOf(
                        "invoiceId" to invoiceId,
                        "documentType" to "INVOICE",
                        "originalPdfSize" to invoicePdfWithoutSignature.size,
                        "replaceOriginalAttachment" to true
                    )
                ),
                createdBy = userId,
                expiresAt = expiresAt
            )

            val savedSession = documentSignatureSessionRepository.save(session)

            val cachedData = CachedSignatureData(
                sessionId = sessionId.toString(),
                signatureImageBase64 = "",
                signatureImageBytes = ByteArray(0),
                originalDocumentBytes = invoicePdfWithoutSignature,
                signedAt = Instant.now(),
                signerName = request.customerName,
                protocolId = null,
                tabletId = request.tabletId,
                companyId = companyId,
                metadata = mapOf(
                    "documentType" to "INVOICE",
                    "invoiceId" to invoiceId,
                    "instructions" to (request.instructions ?: ""),
                    "createdBy" to userId,
                    "replaceOriginalAttachment" to true
                )
            )

            signatureCacheService.cacheSignature(sessionId.toString(), cachedData)

            val documentRequest = DocumentSignatureRequestDto(
                sessionId = sessionId.toString(),
                documentId = session.documentId.toString(),
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = request.signatureTitle,
                documentTitle = "Faktura ${document.number}",
                documentType = "INVOICE",
                pageCount = 1,
                previewUrls = emptyList(),
                instructions = request.instructions,
                businessContext = mapOf(
                    "invoiceId" to invoiceId,
                    "documentType" to "INVOICE"
                ),
                timeoutMinutes = request.timeoutMinutes,
                expiresAt = expiresAt.toString(),
                signatureFields = null
            )

            val sent = webSocketHandler.sendDocumentSignatureRequestWithDocument(
                request.tabletId,
                documentRequest,
                invoicePdfWithoutSignature
            )

            if (sent) {
                documentSignatureSessionRepository.save(
                    savedSession.updateStatus(SignatureSessionStatus.SENT_TO_TABLET)
                )

                notifyFrontendSignatureStarted(companyId, sessionId.toString(), invoiceId)

                logger.info("Invoice signature request sent to tablet successfully: $sessionId")

                return InvoiceSignatureResponse(
                    success = true,
                    sessionId = sessionId,
                    message = "Invoice signature request sent to tablet successfully",
                    invoiceId = invoiceId,
                    expiresAt = expiresAt
                )
            } else {
                documentSignatureSessionRepository.save(
                    savedSession.updateStatus(SignatureSessionStatus.ERROR, "Failed to send to tablet")
                )

                signatureCacheService.removeSignature(sessionId.toString())
                throw InvoiceSignatureException("Failed to send signature request to tablet")
            }

        } catch (e: Exception) {
            logger.error("Error requesting invoice signature", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }

    fun processSignatureFromTablet(sessionId: String, signatureImageBase64: String): Boolean {
        logger.info("Processing invoice signature from tablet for session: $sessionId")

        try {
            val signatureBytes = Base64.getDecoder().decode(
                signatureImageBase64.substringAfter("base64,")
            )

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

            val session = documentSignatureSessionRepository.findBySessionId(UUID.fromString(sessionId))
            if (session != null) {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.COMPLETED)
                )
            }

            // NOWE: Automatyczne zastąpienie oryginalnego załącznika
            val shouldReplaceAttachment = updatedData.metadata["replaceOriginalAttachment"] as? Boolean ?: false
            if (shouldReplaceAttachment) {
                replaceOriginalAttachmentWithSignedVersion(updatedData)
            }

            notifyFrontendSignatureComplete(updatedData.companyId, sessionId, updatedData)

            logger.info("Invoice signature processed and frontend notified for session: $sessionId")
            return true

        } catch (e: Exception) {
            logger.error("Error processing invoice signature from tablet", e)
            return false
        }
    }

    private fun replaceOriginalAttachmentWithSignedVersion(cachedData: CachedSignatureData) {
        try {
            val invoiceId = cachedData.metadata["invoiceId"] as? String ?: return

            logger.info("Replacing original attachment with signed version for invoice: $invoiceId")

            val document = unifiedDocumentService.getDocumentById(invoiceId)
            val signedInvoicePdf = generateInvoicePdfWithSignature(document, cachedData.companyId, cachedData.signatureImageBytes)

            val inputStreamFile: org.springframework.web.multipart.MultipartFile = object : org.springframework.web.multipart.MultipartFile {
                override fun getName(): String = "signed-invoice"
                override fun getOriginalFilename(): String = "signed-invoice-${document.number}.pdf"
                override fun getContentType(): String = "application/pdf"
                override fun isEmpty(): Boolean = signedInvoicePdf.isEmpty()
                override fun getSize(): Long = signedInvoicePdf.size.toLong()
                override fun getBytes(): ByteArray = signedInvoicePdf
                override fun getInputStream(): java.io.InputStream = signedInvoicePdf.inputStream()
                override fun transferTo(dest: java.io.File): Unit = throw UnsupportedOperationException("Transfer not supported")
            }

            // Usuń stary załącznik jeśli istnieje
            if (document.attachment != null) {
                try {
                    universalStorageService.deleteFile(document.attachment!!.storageId)
                    logger.info("Deleted original attachment for invoice: $invoiceId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete original attachment", e)
                }
            }

            // Zapisz nowy podpisany załącznik
            val storageId: String = universalStorageService.storeFile(
                UniversalStoreRequest(
                    file = inputStreamFile,
                    originalFileName = "signed-invoice-${document.number}.pdf",
                    contentType = "application/pdf",
                    companyId = cachedData.companyId,
                    entityId = document.id.value,
                    entityType = "document",
                    category = "finances",
                    subCategory = "invoices/${document.direction.name.lowercase()}",
                    description = "Signed invoice PDF with client signature",
                    date = document.issuedDate,
                    tags = mapOf(
                        "documentType" to document.type.name,
                        "direction" to document.direction.name,
                        "signed" to "true",
                        "signerName" to cachedData.signerName
                    )
                )
            )

            // Zaktualizuj dokument z nowym załącznikiem
            val newAttachment = DocumentAttachment(
                id = UUID.randomUUID().toString(),
                name = "signed-invoice-${document.number}.pdf",
                size = signedInvoicePdf.size.toLong(),
                type = "application/pdf",
                storageId = storageId,
                uploadedAt = LocalDateTime.now()
            )

            val updatedDocument = document.copy(attachment = newAttachment)

            logger.info("Successfully replaced attachment with signed version for invoice: $invoiceId")

        } catch (e: Exception) {
            logger.error("Failed to replace original attachment with signed version", e)
            // Nie przerywamy procesu - podpisana faktura nadal dostępna przez endpoint
        }
    }

    fun getInvoiceSignatureStatus(
        sessionId: UUID,
        companyId: Long,
        invoiceId: String
    ): InvoiceSignatureStatusResponse {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw InvoiceSignatureException("Signature session not found")

        val businessContext = session.businessContext?.let {
            objectMapper.readValue(it, Map::class.java)
        }
        val contextInvoiceId = businessContext?.get("invoiceId") as? String

        if (contextInvoiceId != invoiceId) {
            throw InvoiceSignatureException("Session does not belong to this invoice")
        }

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

        return InvoiceSignatureStatusResponse(
            success = true,
            sessionId = sessionId,
            invoiceId = invoiceId,
            status = mapToInvoiceStatus(currentStatus),
            signedAt = session.signedAt,
            signedInvoiceUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoices/$invoiceId/signed-document/$sessionId" else null,
            signatureImageUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoices/$invoiceId/signature-image/$sessionId" else null,
            timestamp = Instant.now()
        )
    }

    fun getSignedInvoice(sessionId: UUID, companyId: Long, invoiceId: String): ByteArray? {
        val cachedData = signatureCacheService.getSignature(sessionId.toString())

        if (cachedData == null || cachedData.companyId != companyId) {
            logger.warn("Signed invoice not found or access denied for session: $sessionId")
            return null
        }

        val contextInvoiceId = cachedData.metadata["invoiceId"] as? String
        if (contextInvoiceId != invoiceId) {
            logger.warn("Session does not belong to invoice: $invoiceId")
            return null
        }

        if (cachedData.signatureImageBytes.isEmpty()) {
            logger.warn("Signature not yet available for session: $sessionId")
            return null
        }

        return try {
            val document = unifiedDocumentService.getDocumentById(invoiceId)
            val signedInvoicePdf = generateInvoicePdfWithSignature(document, companyId, cachedData.signatureImageBytes)

            logger.info("Signed invoice generated for session: $sessionId")
            signedInvoicePdf

        } catch (e: Exception) {
            logger.error("Error generating signed invoice for session: $sessionId", e)
            null
        }
    }

    fun cancelInvoiceSignatureSession(
        sessionId: UUID,
        companyId: Long,
        invoiceId: String,
        userId: String,
        reason: String?
    ) {
        val session = documentSignatureSessionRepository.findBySessionIdAndCompanyId(sessionId, companyId)
            ?: throw InvoiceSignatureException("Session not found")

        val businessContext = session.businessContext?.let {
            objectMapper.readValue(it, Map::class.java)
        }
        val contextInvoiceId = businessContext?.get("invoiceId") as? String

        if (contextInvoiceId != invoiceId) {
            throw InvoiceSignatureException("Session does not belong to this invoice")
        }

        if (session.status in listOf(SignatureSessionStatus.COMPLETED, SignatureSessionStatus.CANCELLED)) {
            throw InvoiceSignatureException("Session cannot be cancelled (status: ${session.status})")
        }

        val cancelledSession = session.cancel(userId, reason)
        documentSignatureSessionRepository.save(cancelledSession)

        signatureCacheService.removeSignature(sessionId.toString())

        webSocketHandler.notifySessionCancellation(sessionId)

        logger.info("Invoice signature session cancelled: $sessionId by $userId")
    }

    fun getCachedSignatureData(sessionId: String): CachedSignatureData? {
        return signatureCacheService.getSignature(sessionId)
    }

    private fun generateInvoicePdfWithoutSignature(
        document: com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument,
        companyId: Long
    ): ByteArray {
        val activeTemplate = templateRepository.findActiveTemplateForCompany(companyId)
            ?: throw InvoiceSignatureException("No active template found for company")

        val companySettings = companySettingsService.getCompanySettings(companyId)
            ?: throw InvoiceSignatureException("Company settings not found")

        val logoData = getLogoData(companySettings)

        val generationData = InvoiceGenerationData(
            document = document,
            companySettings = companySettings,
            logoData = logoData,
            additionalData = mapOf("client_signature" to "")
        )

        return invoiceTemplateService.generatePdfFromTemplate(activeTemplate, generationData)
    }

    private fun generateInvoicePdfWithSignature(
        document: com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument,
        companyId: Long,
        signatureBytes: ByteArray
    ): ByteArray {
        val activeTemplate = templateRepository.findActiveTemplateForCompany(companyId)
            ?: throw InvoiceSignatureException("No active template found for company")

        val companySettings = companySettingsService.getCompanySettings(companyId)
            ?: throw InvoiceSignatureException("Company settings not found")

        val logoData = getLogoData(companySettings)

        val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)
        val signatureHtml = """<img src="data:image/png;base64,$signatureBase64" alt="Podpis klienta" style="max-width: 200px; max-height: 60px;"/>"""

        val generationData = InvoiceGenerationData(
            document = document,
            companySettings = companySettings,
            logoData = logoData,
            additionalData = mapOf("client_signature" to signatureHtml)
        )

        return invoiceTemplateService.generatePdfFromTemplate(activeTemplate, generationData)
    }

    private fun getLogoData(companySettings: com.carslab.crm.modules.company_settings.domain.model.CompanySettings): ByteArray? {
        return companySettings.logoSettings.logoFileId?.let { logoFileId ->
            try {
                logoStorageService.getLogoPath(logoFileId)?.let { path ->
                    java.nio.file.Files.readAllBytes(path)
                }
            } catch (e: Exception) {
                logger.warn("Failed to load logo for invoice generation", e)
                null
            }
        }
    }

    private fun notifyFrontendSignatureStarted(companyId: Long, sessionId: String, invoiceId: String) {
        val notification = mapOf(
            "type" to "invoice_signature_started",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "invoiceId" to invoiceId,
                "status" to "SENT_TO_TABLET",
                "timestamp" to Instant.now()
            )
        )

        webSocketHandler.broadcastToWorkstations(companyId, notification)
    }

    private fun notifyFrontendSignatureComplete(companyId: Long, sessionId: String, cachedData: CachedSignatureData) {
        val notification = mapOf(
            "type" to "invoice_signature_ready",
            "payload" to mapOf(
                "sessionId" to sessionId,
                "invoiceId" to cachedData.metadata["invoiceId"],
                "success" to true,
                "attachmentReplaced" to (cachedData.metadata["replaceOriginalAttachment"] as? Boolean ?: false),
                "signedInvoiceUrl" to "/api/invoices/${cachedData.metadata["invoiceId"]}/signed-document/$sessionId",
                "signatureImageUrl" to "/api/invoices/${cachedData.metadata["invoiceId"]}/signature-image/$sessionId",
                "signedAt" to cachedData.signedAt,
                "signerName" to cachedData.signerName,
                "timestamp" to Instant.now()
            )
        )

        webSocketHandler.broadcastToWorkstations(companyId, notification)
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

class InvoiceSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)