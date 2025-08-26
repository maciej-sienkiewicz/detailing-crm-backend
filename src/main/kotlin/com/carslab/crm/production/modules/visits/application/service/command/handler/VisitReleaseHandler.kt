package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.api.model.DocumentItemDTO
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.OverridenInvoiceServiceItem
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class VisitReleaseHandler(
    private val visitDomainService: VisitDomainService,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val unifiedDocumentService: UnifiedDocumentService,
    private val securityContext: SecurityContext
) {

    fun handle(visitId: String, request: ReleaseVehicleRequest, companyId: Long): Boolean {
        val visit = visitDetailQueryService.getVisitDetail(visitId)
        val companyDetails = companyDetailsFetchService.getCompanySettings(companyId)

        val items = createDocumentItems(visit, request)
        val totals = calculateTotals(items)

        completeVisit(visitId, companyId)
        createDocument(visitId, visit, request, companyDetails, items, totals)

        return true
    }

    private fun createDocumentItems(
        visit: com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto,
        request: ReleaseVehicleRequest
    ): List<DocumentItemDTO> {
        return if (!request.overridenItems.isNullOrEmpty()) {
            createOverriddenItems(request.overridenItems)
        } else {
            createServiceItems(visit.selectedServices)
        }
    }

    private fun createOverriddenItems(overriddenItems: List<CreateServiceCommand>): List<DocumentItemDTO> {
        return overriddenItems.map { item ->
            val finalPrice = CalculationUtils.anyToBigDecimal(item.finalPrice ?: item.price)
            val quantity = CalculationUtils.anyToBigDecimal(item.quantity)
            val totalGross = finalPrice.multiply(quantity)
            val totalNet = CalculationUtils.calculateNetAmount(totalGross)

            DocumentItemDTO(
                id = null,
                name = item.name,
                description = null,
                quantity = quantity,
                unitPrice = finalPrice,
                taxRate = BigDecimal("23"),
                totalNet = totalNet,
                totalGross = totalGross
            )
        }
    }

    private fun createServiceItems(services: List<com.carslab.crm.modules.visits.api.commands.ServiceDto>): List<DocumentItemDTO> {
        return services.map { service ->
            val finalPrice = CalculationUtils.anyToBigDecimal(service.finalPrice)
            val totalNet = CalculationUtils.calculateNetAmount(finalPrice)

            DocumentItemDTO(
                id = null,
                name = service.name,
                description = service.note,
                quantity = BigDecimal.ONE,
                unitPrice = finalPrice,
                taxRate = BigDecimal("23"),
                totalNet = totalNet,
                totalGross = finalPrice
            )
        }
    }

    private fun calculateTotals(items: List<DocumentItemDTO>): DocumentTotals {
        val totalGross = items.sumOf { it.totalGross }
        val totalNet = items.sumOf { it.totalNet }
        val totalTax = CalculationUtils.calculateVatFromGrossNet(totalGross, totalNet)

        return DocumentTotals(totalGross, totalNet, totalTax)
    }

    private fun completeVisit(visitId: String, companyId: Long) {
        val existingVisit = visitDomainService.getVisitForCompany(VisitId(visitId.toLong()), companyId)
        val completedVisit = existingVisit.copy(status = VisitStatus.COMPLETED)
        visitDomainService.updateVisit(VisitId(visitId.toLong()),
            com.carslab.crm.production.modules.visits.domain.command.UpdateVisitCommand(
                title = completedVisit.title,
                startDate = completedVisit.period.startDate,
                endDate = completedVisit.period.endDate,
                services = emptyList(),
                notes = completedVisit.notes,
                referralSource = completedVisit.referralSource,
                appointmentId = completedVisit.appointmentId,
                calendarColorId = completedVisit.calendarColorId,
                keysProvided = completedVisit.documents.keysProvided,
                documentsProvided = completedVisit.documents.documentsProvided,
                status = VisitStatus.COMPLETED,
                deliveryPerson = completedVisit.deliveryPerson
            ), companyId)
    }

    private fun createDocument(
        visitId: String,
        visit: com.carslab.crm.modules.visits.api.commands.CarReceptionDetailDto,
        request: ReleaseVehicleRequest,
        companyDetails: CompanySettingsResponse,
        items: List<DocumentItemDTO>,
        totals: DocumentTotals
    ) {
        unifiedDocumentService.createDocument(
            request = CreateUnifiedDocumentRequest(
                type = request.documentType,
                title = "Opłata za wizytę - ${visit.title}",
                description = request.additionalNotes,
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(request.paymentDays),
                sellerName = companyDetails.basicInfo?.companyName ?: "",
                sellerTaxId = companyDetails.basicInfo?.taxId ?: "",
                sellerAddress = companyDetails.basicInfo?.address ?: "",
                buyerName = visit.ownerName,
                buyerTaxId = visit.taxId,
                buyerAddress = visit.address,
                status = "NOT_PAID",
                direction = "INCOME",
                paymentMethod = request.paymentMethod,
                totalNet = totals.totalNet,
                totalTax = totals.totalTax,
                totalGross = totals.totalGross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                items = items,
                notes = request.additionalNotes ?: "",
                protocolId = null,
                protocolNumber = null,
                visitId = visitId
            ),
            attachmentFile = null
        )
    }

    private data class DocumentTotals(
        val totalGross: BigDecimal,
        val totalNet: BigDecimal,
        val totalTax: BigDecimal
    )
}