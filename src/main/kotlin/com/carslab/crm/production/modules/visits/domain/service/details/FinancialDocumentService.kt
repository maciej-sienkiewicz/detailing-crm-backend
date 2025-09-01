package com.carslab.crm.production.modules.visits.domain.service.details

import com.carslab.crm.api.model.DocumentItemDTO
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.service.aggregate.VisitCompletionService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FinancialDocumentService(
    private val unifiedDocumentService: UnifiedDocumentService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val visitDetailQueryService: VisitDetailQueryService
) {

    fun createVisitDocument(command: VisitCompletionService.FinancialDocumentCommand) {
        val visit = visitDetailQueryService.getVisitDetail(command.visitId)
        val companyDetails = companyDetailsFetchService.getCompanySettings(command.companyId)

        val documentItems = command.items.map { item ->
            DocumentItemDTO(
                id = null,
                name = item.name,
                description = null,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                taxRate = BigDecimal("23"),
                totalNet = item.totalNet,
                totalGross = item.totalGross
            )
        }

        unifiedDocumentService.createDocument(
            request = CreateUnifiedDocumentRequest(
                type = command.documentType,
                title = "Opłata za wizytę - ${command.visitTitle}",
                description = command.additionalNotes,
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(command.paymentDays.toLong()),
                sellerName = companyDetails.basicInfo?.companyName ?: "",
                sellerTaxId = companyDetails.basicInfo?.taxId ?: "",
                sellerAddress = companyDetails.basicInfo?.address ?: "",
                buyerName = visit.ownerName,
                buyerTaxId = visit.taxId,
                buyerAddress = visit.address,
                status = command.status.toString(),
                direction = "INCOME",
                paymentMethod = command.paymentMethod,
                totalNet = command.totals.totalNet,
                totalTax = command.totals.totalTax,
                totalGross = command.totals.totalGross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                items = documentItems,
                notes = command.additionalNotes ?: "",
                protocolId = null,
                protocolNumber = null,
                visitId = command.visitId
            ),
            attachmentFile = null
        )
    }
}