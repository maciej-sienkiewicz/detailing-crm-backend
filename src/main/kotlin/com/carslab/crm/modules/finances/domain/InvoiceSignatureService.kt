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
    private val invoiceSignatureDomainService: InvoiceSignatureDomainService,
    private val unifiedDocumentService: UnifiedDocumentService,
    private val documentSignatureSessionRepository: DocumentSignatureSessionRepository,
    private val tabletDeviceRepository: TabletDeviceRepository,
    private val webSocketService: WebSocketService,
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

        validateTabletAccess(request.tabletId, companyId)

        val protocol = try {
            protocolRepository.findById(ProtocolId(visitId))
                ?: throw InvoiceSignatureException("Visit not found: $visitId")
        } catch (e: Exception) {
            throw InvoiceSignatureException("Error retrieving visit: ${e.message}", e)
        }

        if (protocol.status != ProtocolStatus.READY_FOR_PICKUP && protocol.status != ProtocolStatus.COMPLETED) {
            throw InvoiceSignatureException("Visit must be in READY_FOR_PICKUP or COMPLETED status to generate invoice")
        }

        val document = findOrCreateInvoiceFromVisit(protocol, companyId)

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

        validateTabletAccess(request.tabletId, companyId)

        val document = unifiedDocumentService.getDocumentById(invoiceId)
        val expiresAt = Instant.now().plus(request.timeoutMinutes.toLong(), ChronoUnit.MINUTES)

        try {
            val session = invoiceSignatureDomainService.createSignatureSession(
                invoiceId = invoiceId,
                tabletId = request.tabletId,
                companyId = companyId,
                signerName = request.customerName,
                signatureTitle = request.signatureTitle,
                instructions = request.instructions,
                userId = userId,
                timeoutMinutes = request.timeoutMinutes
            )

            val metadata = mapOf(
                "documentType" to "INVOICE",
                "invoiceId" to invoiceId,
                "instructions" to (request.instructions ?: ""),
                "createdBy" to userId,
                "replaceOriginalAttachment" to true,
                "document" to document
            )

            val invoicePdfBytes = invoiceSignatureDomainService.cacheInvoiceForSignature(
                document = document,
                sessionId = session.sessionId.toString(),
                tabletId = request.tabletId.toString(),
                companyId = companyId,
                signerName = request.customerName,
                metadata = metadata
            )

            val sent = invoiceSignatureDomainService.sendDocumentSignatureRequest(
                session = session,
                document = document,
                invoicePdfBytes = invoicePdfBytes
            )

            if (sent) {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.SENT_TO_TABLET)
                )

                notifyFrontendSignatureStarted(companyId, session.sessionId.toString(), invoiceId)

                logger.info("Invoice signature request sent to tablet successfully: ${session.sessionId}")

                return InvoiceSignatureResponse(
                    success = true,
                    sessionId = session.sessionId,
                    message = "Invoice signature request sent to tablet successfully",
                    invoiceId = invoiceId,
                    expiresAt = expiresAt
                )
            } else {
                documentSignatureSessionRepository.save(
                    session.updateStatus(SignatureSessionStatus.ERROR, "Failed to send to tablet")
                )

                throw InvoiceSignatureException("Failed to send signature request to tablet")
            }

        } catch (e: Exception) {
            logger.error("Error requesting invoice signature", e)
            throw InvoiceSignatureException("Failed to request invoice signature: ${e.message}", e)
        }
    }

    fun processSignatureFromTablet(sessionId: String, signatureImageBase64: String): Boolean {
        return invoiceSignatureDomainService.processSignatureFromTabletNotReplace( sessionId, signatureImageBase64)
    }

    fun processSignatureFromTablet(sessionId: String): Boolean {
        return invoiceSignatureDomainService.processSignatureFromTablet(sessionId, getCachedSignatureData(sessionId)?.signatureImageBase64 ?: throw InvoiceSignatureException("Signature image not found"))
    }


    fun getInvoiceSignatureStatus(
        sessionId: UUID,
        companyId: Long,
        invoiceId: String
    ): InvoiceSignatureStatusResponse {
        return invoiceSignatureDomainService.getInvoiceSignatureStatus(sessionId, companyId, invoiceId)
    }

    fun getSignedInvoice(sessionId: UUID, companyId: Long, invoiceId: String): ByteArray? {
        return invoiceSignatureDomainService.getSignedInvoice(sessionId, companyId, invoiceId)
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

        webSocketService.notifySessionCancellation(sessionId)

        logger.info("Invoice signature session cancelled: $sessionId by $userId")
    }

    fun getCachedSignatureData(sessionId: String): CachedSignatureData? {
        return invoiceSignatureDomainService.getCachedSignatureData(sessionId)
    }

    private fun validateTabletAccess(tabletId: UUID, companyId: Long) {
        val tablet = tabletDeviceRepository.findById(tabletId).orElse(null)
            ?: throw InvoiceSignatureException("Tablet not found: $tabletId")

        if (tablet.companyId != companyId) {
            throw InvoiceSignatureException("Tablet does not belong to company")
        }

        if (!webSocketService.isTabletConnected(tabletId)) {
            throw InvoiceSignatureException("Tablet is offline")
        }
    }

    private fun findOrCreateInvoiceFromVisit(
        protocol: com.carslab.crm.domain.model.CarReceptionProtocol,
        companyId: Long
    ): UnifiedFinancialDocument {
        val existingInvoice = findExistingInvoiceForVisit(protocol.id.value, companyId)
        if (existingInvoice != null) {
            return existingInvoice
        }

        return createInvoiceFromVisit(protocol, companyId)
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
}

class InvoiceSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)