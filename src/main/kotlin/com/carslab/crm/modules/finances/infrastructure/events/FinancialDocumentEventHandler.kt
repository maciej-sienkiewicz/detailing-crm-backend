package com.carslab.crm.modules.finances.infrastructure.events

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.DocumentType
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.modules.company_settings.domain.CompanySettingsApplicationService
import com.carslab.crm.modules.finances.domain.InvoiceAttachmentGenerationService
import com.carslab.crm.modules.visits.domain.events.VehicleServiceCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class FinancialDocumentEventHandler(
    private val unifiedDocumentRepository: UnifiedDocumentRepository,
    private val securityContext: SecurityContext,
    private val companySettingsApplicationService: CompanySettingsApplicationService,
    private val invoiceAttachmentGenerationService: InvoiceAttachmentGenerationService
) {
    private val logger = LoggerFactory.getLogger(FinancialDocumentEventHandler::class.java)

    @EventListener
    @Transactional
    fun handleVehicleServiceCompleted(event: VehicleServiceCompletedEvent) {
        logger.info("Creating financial document for completed service: ${event.protocolId}")
    }

    private fun createFinancialDocument(
        event: VehicleServiceCompletedEvent,
        companySettings: CompanySettingsResponse
    ): UnifiedFinancialDocument {
        val items = event.services.map { service ->
            DocumentItem(
                name = service.name,
                description = service.description,
                quantity = service.quantity,
                unitPrice = service.unitPrice,
                taxRate = service.taxRate,
                totalNet = service.totalNet,
                totalGross = service.totalGross
            )
        }

        return UnifiedFinancialDocument(
            id = UnifiedDocumentId.generate(),
            number = "",
            type = when (event.documentType.lowercase()) {
                "invoice" -> DocumentType.INVOICE
                "receipt" -> DocumentType.RECEIPT
                else -> DocumentType.OTHER
            },
            title = "Faktura za wizytę: ${event.protocolTitle}",
            description = "",
            issuedDate = LocalDate.now(),
            dueDate = LocalDate.now().plusDays(event.paymentIn),
            sellerName = companySettings.basicInfo?.companyName ?: "Brak nazwy firmy",
            sellerTaxId = companySettings.basicInfo?.taxId ?: "",
            sellerAddress = companySettings.basicInfo?.address ?: "Brak adresu",
            buyerName = event.clientName,
            buyerTaxId = event.clientTaxId,
            buyerAddress = event.clientAddress ?: "Brak adresu",
            status = when(event.paymentMethod.lowercase()) {
                "cash", "card" -> DocumentStatus.PAID
                else -> DocumentStatus.NOT_PAID
            },
            direction = TransactionDirection.INCOME,
            paymentMethod = when (event.paymentMethod.lowercase()) {
                "cash" -> PaymentMethod.CASH
                "card" -> PaymentMethod.CARD
                else -> PaymentMethod.BANK_TRANSFER
            },
            totalNet = event.totalNet,
            totalTax = event.totalTax,
            totalGross = event.totalGross,
            paidAmount = if (event.paymentMethod.lowercase() in listOf("cash", "card")) event.totalGross else BigDecimal.ZERO,
            currency = "PLN",
            notes = "Dokument utworzony automatycznie przy zakończeniu wizyty",
            protocolId = event.protocolId,
            protocolNumber = event.protocolId,
            visitId = null,
            items = items,
            attachment = null,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
    }
}