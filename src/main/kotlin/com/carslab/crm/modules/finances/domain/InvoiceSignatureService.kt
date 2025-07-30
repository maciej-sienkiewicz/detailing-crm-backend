package com.carslab.crm.modules.finances.domain

import com.carslab.crm.domain.model.view.finance.DocumentAttachment
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureResponse
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatus
import com.carslab.crm.modules.finances.api.responses.InvoiceSignatureStatusResponse
import com.carslab.crm.signature.infrastructure.persistance.entity.DocumentSignatureSession
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentSignatureSessionRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.TabletDeviceRepository
import com.carslab.crm.signature.websocket.SignatureWebSocketHandler
import com.carslab.crm.signature.service.SignatureCacheService
import com.carslab.crm.signature.service.CachedSignatureData
import com.carslab.crm.signature.api.dto.DocumentSignatureRequestDto
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.signature.service.WebSocketService
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
@Transactional
class InvoiceSignatureService(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val invoiceAttachmentGenerationService: InvoiceAttachmentGenerationService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketService: WebSocketService,
    private val signatureCacheService: SignatureCacheService,
    private val universalStorageService: UniversalStorageService,
    private val objectMapper: ObjectMapper,
    private val protocolRepository: ProtocolRepository,
    private val companySettingsService: CompanySettingsDomainService,
    private val unifiedDocumentRepository: UnifiedDocumentRepository
) {
    private val logger = LoggerFactory.getLogger(InvoiceSignatureService::class.java)

    fun requestInvoiceSignatureFromVisit(
        companyId: Long,
        userId: String,
        visitId: String,
        request: InvoiceSignatureRequest
    ): InvoiceSignatureResponse {
        logger.info("Requesting invoice signature from visit: {} by user: {}", visitId, userId)

        val tablet = tabletDeviceRepository.findById(request.tabletId).orElse(null)
            ?: throw InvoiceSignatureException("Tablet not found: ${request.tabletId}")

        if (tablet.companyId != companyId) {
            throw InvoiceSignatureException("Tablet does not belong to company")
        }

        if (!webSocketService.isTabletConnected(request.tabletId)) {
            throw InvoiceSignatureException("Tablet is offline")
        }

        val protocol = try {
            protocolRepository.findById(ProtocolId(visitId))
                ?: throw InvoiceSignatureException("Visit not found: $visitId")
        } catch (e: Exception) {
            throw InvoiceSignatureException("Error retrieving visit: ${e.message}", e)
        }

        if (protocol.status != ProtocolStatus.READY_FOR_PICKUP && protocol.status != ProtocolStatus.COMPLETED) {
            throw InvoiceSignatureException("Visit must be in READY_FOR_PICKUP or COMPLETED status to generate invoice")
        }

        val document = createInvoiceFromVisit(protocol, companyId)

        return requestInvoiceSignature(
            companyId = companyId,
            userId = userId,
            invoiceId = document.id.value,
            request = request
        )
    }

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

        if (!webSocketService.isTabletConnected(request.tabletId)) {
            throw InvoiceSignatureException("Tablet is offline")
        }

        val document = unifiedDocumentService.getDocumentById(invoiceId)
        val sessionId = UUID.randomUUID()
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        try {
            val invoicePdfBytes = getOrGenerateUnsignedInvoicePdf(document, companyId)

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
                        "originalPdfSize" to invoicePdfBytes.size,
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
                originalDocumentBytes = invoicePdfBytes,
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

            val sent = webSocketService.sendDocumentSignatureRequestWithDocument(
                request.tabletId,
                documentRequest,
                invoicePdfBytes
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

            if (document.attachment != null) {
                try {
                    universalStorageService.deleteFile(document.attachment!!.storageId)
                    logger.info("Deleted original attachment for invoice: $invoiceId")
                } catch (e: Exception) {
                    logger.warn("Failed to delete original attachment", e)
                }
            }

            val signedAttachment = invoiceAttachmentGenerationService.generateSignedInvoiceAttachment(
                document,
                cachedData.signatureImageBytes
            )

            if (signedAttachment != null) {
                val updatedDocument = document.copy(attachment = signedAttachment)
                unifiedDocumentService.updateDocumentWithAttachment(updatedDocument)
                logger.info("Successfully replaced attachment with signed version for invoice: $invoiceId")
            } else {
                logger.error("Failed to generate signed attachment for invoice: $invoiceId")
            }

        } catch (e: Exception) {
            logger.error("Failed to replace original attachment with signed version", e)
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
                "/api/invoices/$invoiceId/signature/$sessionId/signed-document" else null,
            signatureImageUrl = if (currentStatus == SignatureSessionStatus.COMPLETED)
                "/api/invoices/$invoiceId/signature/$sessionId/signature-image" else null,
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
            val signedAttachment = invoiceAttachmentGenerationService.generateSignedInvoiceAttachment(
                document,
                cachedData.signatureImageBytes
            )

            if (signedAttachment != null) {
                universalStorageService.retrieveFile(signedAttachment.storageId)
            } else {
                null
            }

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

        webSocketService.notifySessionCancellation(sessionId)

        logger.info("Invoice signature session cancelled: $sessionId by $userId")
    }

    fun getCachedSignatureData(sessionId: String): CachedSignatureData? {
        return signatureCacheService.getSignature(sessionId)
    }

    private fun findExistingInvoiceForVisit(visitId: String, companyId: Long): UnifiedFinancialDocument? {
        return try {
            val documents = unifiedDocumentRepository.findInvoicesByCompanyAndDateRange(
                companyId = companyId,
                startDate = LocalDate.now().minusDays(30),
                endDate = LocalDate.now()
            )

            documents.find { document ->
                document.protocolId == visitId || document.visitId == visitId
            }
        } catch (e: Exception) {
            logger.warn("Error searching for existing invoice for visit: $visitId", e)
            null
        }
    }

    private fun createInvoiceFromVisit(
        protocol: com.carslab.crm.domain.model.CarReceptionProtocol,
        companyId: Long
    ): UnifiedFinancialDocument {
        logger.info("Creating invoice from visit: ${protocol.id.value}")

        val companySettings = try {
            companySettingsService.getCompanySettings(companyId)
                ?: throw InvoiceSignatureException("Company settings not found for company: $companyId")
        } catch (e: Exception) {
            throw InvoiceSignatureException("Error retrieving company settings: ${e.message}", e)
        }

        val approvedServices = protocol.protocolServices.filter {
            it.approvalStatus == ApprovalStatus.APPROVED
        }

        if (approvedServices.isEmpty()) {
            throw InvoiceSignatureException("No approved services found for visit")
        }

        val items = approvedServices.map { service ->
            DocumentItem(
                id = UUID.randomUUID().toString(),
                name = service.name,
                description = service.note,
                quantity = service.quantity.toBigDecimal(),
                unitPrice = service.finalPrice.amount.toBigDecimal(),
                taxRate = BigDecimal("23"),
                totalNet = (service.finalPrice.amount.toBigDecimal() / BigDecimal("1.23")).setScale(2, RoundingMode.HALF_UP),
                totalGross = service.finalPrice.amount.toBigDecimal()
            )
        }

        val totalGross = items.sumOf { it.totalGross }
        val totalNet = items.sumOf { it.totalNet }
        val totalTax = totalGross - totalNet

        val document = UnifiedFinancialDocument(
            id = UnifiedDocumentId.generate(),
            number = generateInvoiceNumber(companyId),
            type = DocumentType.INVOICE,
            title = "Faktura za wizytę: ${protocol.title}",
            description = "Automatycznie wygenerowana faktura z wizyty",
            issuedDate = LocalDate.now(),
            dueDate = LocalDate.now().plusDays(14),
            sellerName = companySettings.basicInfo.companyName,
            sellerTaxId = companySettings.basicInfo.taxId,
            sellerAddress = companySettings.basicInfo.address ?: "",
            buyerName = protocol.client.name,
            buyerTaxId = protocol.client.taxId,
            buyerAddress = protocol.client.address ?: "",
            status = DocumentStatus.NOT_PAID,
            direction = TransactionDirection.INCOME,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            paidAmount = BigDecimal.ZERO,
            currency = "PLN",
            notes = "Faktura wygenerowana automatycznie dla podpisu",
            protocolId = protocol.id.value,
            protocolNumber = protocol.id.value,
            visitId = protocol.id.value,
            items = items,
            attachment = null,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        return try {
            unifiedDocumentRepository.save(document)
        } catch (e: Exception) {
            logger.error("Error creating invoice from visit: ${protocol.id.value}", e)
            throw InvoiceSignatureException("Failed to create invoice: ${e.message}", e)
        }
    }

    private fun generateInvoiceNumber(companyId: Long): String {
        val year = LocalDate.now().year
        val month = LocalDate.now().monthValue

        return try {
            unifiedDocumentRepository.generateDocumentNumber(
                year = year,
                month = month,
                type = DocumentType.INVOICE.name,
                direction = TransactionDirection.INCOME.name
            )
        } catch (e: Exception) {
            logger.warn("Error generating invoice number, using fallback", e)
            "FV/$year/$month/${System.currentTimeMillis()}"
        }
    }

    private fun getOrGenerateUnsignedInvoicePdf(
        document: com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument,
        companyId: Long
    ): ByteArray {
        return document.attachment?.let { attachment ->
            try {
                universalStorageService.retrieveFile(attachment.storageId)
            } catch (e: Exception) {
                logger.warn("Failed to retrieve existing attachment, generating new one", e)
                null
            }
        } ?: run {
            val unsignedAttachment = invoiceAttachmentGenerationService.generateInvoiceAttachmentWithoutSignature(document)
                ?: throw InvoiceSignatureException("Failed to generate unsigned invoice PDF")
            universalStorageService.retrieveFile(unsignedAttachment.storageId)
                ?: throw InvoiceSignatureException("Failed to retrieve generated invoice PDF")
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

        webSocketService.broadcastToWorkstations(companyId, notification)
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

        webSocketService.broadcastToWorkstations(companyId, notification)
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