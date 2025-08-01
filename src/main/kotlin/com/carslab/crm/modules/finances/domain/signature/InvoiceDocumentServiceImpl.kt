package com.carslab.crm.modules.finances.domain.signature

import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.modules.finances.domain.signature.ports.InvoiceDocumentService
import com.carslab.crm.modules.finances.domain.signature.model.InvoiceSignatureException
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.modules.finances.api.requests.InvoiceSignatureRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InvoiceDocumentServiceImpl(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val protocolRepository: ProtocolRepository,
    private val companySettingsService: CompanySettingsDomainService,
    private val unifiedDocumentRepository: UnifiedDocumentRepository
) : InvoiceDocumentService {

    private val logger = LoggerFactory.getLogger(InvoiceDocumentServiceImpl::class.java)

    override fun getDocument(invoiceId: String): UnifiedFinancialDocument {
        return try {
            unifiedDocumentService.getDocumentById(invoiceId)
        } catch (e: Exception) {
            logger.error("Failed to get document: $invoiceId", e)
            throw InvoiceSignatureException("Document not found: $invoiceId", e)
        }
    }

    override fun findOrCreateInvoiceFromVisit(visitId: String, companyId: Long, request: InvoiceSignatureRequest): UnifiedFinancialDocument {
        logger.info("Finding or creating invoice from visit: $visitId")

        val existingInvoice = findExistingInvoiceForVisit(visitId, companyId)
        if (existingInvoice != null) {
            logger.info("Found existing invoice for visit: $visitId")
            return existingInvoice
        }

        val protocol = protocolRepository.findById(ProtocolId(visitId))
            ?: throw InvoiceSignatureException("Visit not found: $visitId")

        if (protocol.status != ProtocolStatus.READY_FOR_PICKUP && protocol.status != ProtocolStatus.COMPLETED) {
            throw InvoiceSignatureException("Visit must be in READY_FOR_PICKUP or COMPLETED status to generate invoice")
        }

        return createInvoiceFromVisit(protocol, companyId, request)
    }

    private fun findExistingInvoiceForVisit(visitId: String, companyId: Long): UnifiedFinancialDocument? {
        return try {
            val documents = unifiedDocumentRepository.findInvoicesByCompanyAndDateRange(
                companyId = companyId,
                startDate = java.time.LocalDate.now().minusDays(30),
                endDate = java.time.LocalDate.now()
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
        companyId: Long,
        request: InvoiceSignatureRequest
    ): UnifiedFinancialDocument {
        logger.info("Creating invoice from visit: ${protocol.id.value}")

        val companySettings = companySettingsService.getCompanySettings(companyId)
            ?: throw InvoiceSignatureException("Company settings not found for company: $companyId")

        val approvedServices = protocol.protocolServices.filter {
            it.approvalStatus == com.carslab.crm.domain.model.ApprovalStatus.APPROVED
        }

        if (approvedServices.isEmpty()) {
            throw InvoiceSignatureException("No approved services found for visit")
        }

        val items = if(request.overridenItems.isEmpty()) { approvedServices
            .map { DocumentItem(
                name = it.name,
                description = it.note,
                quantity = 1.toBigDecimal(),
                unitPrice = it.finalPrice.amount.toBigDecimal(),
                taxRate = 23.toBigDecimal(),
                totalNet = (it.finalPrice.amount / 1.23).toBigDecimal(),
                totalGross = it.finalPrice.amount.toBigDecimal()
            )} } else request.overridenItems.map {
            DocumentItem(
                name = it.name,
                description = null,
                quantity = 1.toBigDecimal(),
                unitPrice = it.basePrice.toBigDecimal(),
                taxRate = 23.toBigDecimal(),
                totalNet = (it.finalPrice ?: it.basePrice).toBigDecimal() / 1.23.toBigDecimal(),
                totalGross = (it.finalPrice ?: it.basePrice).toBigDecimal()
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
            description = "Faktura wygenerowana przy zakończeniu wizyty",
            issuedDate = java.time.LocalDate.now(),
            dueDate = java.time.LocalDate.now().plusDays(request.paymentDays),
            sellerName = companySettings.basicInfo.companyName,
            sellerTaxId = companySettings.basicInfo.taxId,
            sellerAddress = companySettings.basicInfo.address ?: "",
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
            notes = "Faktura wygenerowana przy zakończeniu wizyty",
            protocolId = protocol.id.value,
            protocolNumber = protocol.id.value,
            visitId = protocol.id.value,
            items = items,
            attachment = null,
            audit = com.carslab.crm.domain.model.Audit(
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
        )

        return try {
            unifiedDocumentRepository.save(document)
        } catch (e: Exception) {
            logger.error("Error creating invoice from visit: ${protocol.id.value}", e)
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
