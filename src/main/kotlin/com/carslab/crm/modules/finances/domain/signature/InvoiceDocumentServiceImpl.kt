package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.modules.finances.domain.signature.ports.InvoiceDocumentService
import com.carslab.crm.modules.finances.domain.signature.model.InvoiceSignatureException
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.visits.application.queries.models.VisitDetailReadModel
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Handles invoice document operations for signature process
 */
@Service
@Transactional
class InvoiceDocumentServiceImpl(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val detailsFetchService: VisitDetailQueryService,
    private val companySettingsService: CompanyDetailsFetchService,
    private val unifiedDocumentRepository: UnifiedDocumentRepository
) : InvoiceDocumentService {

    private val logger = LoggerFactory.getLogger(InvoiceDocumentServiceImpl::class.java)

    override fun getDocument(invoiceId: String, authContext: AuthContext): UnifiedFinancialDocument {
        return try {
            unifiedDocumentService.getDocumentById(invoiceId, authContext)
        } catch (e: Exception) {
            logger.error("Failed to get document: $invoiceId", e)
            throw InvoiceSignatureException("Document not found: $invoiceId", e)
        }
    }

    override fun createInvoiceFromVisit(visitId: String, request: InvoiceSignatureRequest, authContext: AuthContext): UnifiedFinancialDocument {
        logger.info("Finding or creating invoice from visit: $visitId")
        
        val protocol = detailsFetchService.getSimpleDetails(visitId)

        val status = protocol.status.let { ProtocolStatus.valueOf(it) }
        if (status != ProtocolStatus.READY_FOR_PICKUP && status != ProtocolStatus.COMPLETED) {
            throw InvoiceSignatureException("Visit must be in READY_FOR_PICKUP or COMPLETED status to generate invoice")
        }

        return createInvoiceFromVisit(protocol, authContext, request)
    }
    
    private fun createInvoiceFromVisit(
        protocol: VisitDetailReadModel,
        authContext: AuthContext,
        request: InvoiceSignatureRequest
    ): UnifiedFinancialDocument {
        logger.info("Creating invoice from visit: ${protocol.id}")

        val companySettings = companySettingsService.getCompanySettings(authContext.companyId.value)
            ?: throw InvoiceSignatureException("Company settings not found for company: ${authContext.companyId.value}")

        val approvedServices = protocol.services.filter {
            it.approvalStatus == "APPROVED"
        }

        if (approvedServices.isEmpty()) {
            throw InvoiceSignatureException("No approved services found for visit")
        }

        val items = if(request.overridenItems.isEmpty()) { approvedServices
            .map { DocumentItem(
                name = it.name,
                description = it.note,
                quantity = 1.toBigDecimal(),
                unitPrice = it.finalPrice,
                taxRate = 23.toBigDecimal(),
                totalNet = (it.finalPrice / 1.23.toBigDecimal()),
                totalGross = it.finalPrice,
            )} } else request.overridenItems.map {
            DocumentItem(
                name = it.name,
                description = null,
                quantity = 1.toBigDecimal(),
                unitPrice = (it.finalPrice ?: it.basePrice),
                taxRate = 23.toBigDecimal(),
                totalNet = (it.finalPrice ?: it.basePrice) / 1.23.toBigDecimal(),
                totalGross = (it.finalPrice ?: it.basePrice)
            )
        }
        val totalGross = items.sumOf { it.totalGross }
        val totalNet = items.sumOf { it.totalNet }
        val totalTax = totalGross - totalNet

        val document = UnifiedFinancialDocument(
            id = com.carslab.crm.domain.model.view.finance.UnifiedDocumentId.generate(),
            number = generateInvoiceNumber(),
            type = com.carslab.crm.api.model.DocumentType.INVOICE,
            title = "Faktura za wizytę: ${protocol.title}",
            description = "Automatycznie wygenerowana faktura z wizyty",
            issuedDate = java.time.LocalDate.now(),
            dueDate = java.time.LocalDate.now().plusDays(14),
            sellerName = companySettings.basicInfo?.companyName ?: "",
            sellerTaxId = companySettings.basicInfo?.taxId,
            sellerAddress = companySettings.basicInfo?.address ?: "",
            buyerName = protocol.client.name,
            buyerTaxId = protocol.client.taxId,
            buyerAddress = protocol.client.address ?: "",
            status = com.carslab.crm.api.model.DocumentStatus.NOT_PAID,
            direction = com.carslab.crm.api.model.TransactionDirection.INCOME,
            paymentMethod = com.carslab.crm.domain.model.view.finance.PaymentMethod.BANK_TRANSFER,
            totalNet = totalNet,
            totalTax = totalTax,
            totalGross = totalGross,
            paidAmount = java.math.BigDecimal.ZERO,
            currency = "PLN",
            notes = "Faktura wygenerowana automatycznie dla podpisu",
            protocolId = protocol.id,
            protocolNumber = protocol.id,
            visitId = protocol.id,
            items = items,
            attachment = null,
            audit = com.carslab.crm.domain.model.Audit(
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        return try {
            unifiedDocumentRepository.save(document, authContext)
        } catch (e: Exception) {
            logger.error("Error creating invoice from visit: ${protocol.id}", e)
            throw InvoiceSignatureException("Failed to create invoice: ${e.message}", e)
        }
    }

    private fun generateInvoiceNumber(): String {
        val year = java.time.LocalDate.now().year
        val month = java.time.LocalDate.now().monthValue

        return try {
            unifiedDocumentRepository.generateDocumentNumber(
                year = year,
                month = month,
                type = com.carslab.crm.api.model.DocumentType.INVOICE.name,
                direction = com.carslab.crm.api.model.TransactionDirection.INCOME.name
            )
        } catch (e: Exception) {
            logger.warn("Error generating invoice number, using fallback", e)
            "FV/$year/$month/${System.currentTimeMillis()}"
        }
    }
}
