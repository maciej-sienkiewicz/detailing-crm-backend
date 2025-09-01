package com.carslab.crm.production.modules.visits.domain.service.aggregate

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.modules.email.domain.services.EmailSendingService
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.application.service.VehicleStatisticsCommandService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.policy.VisitBusinessPolicy
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.details.FinancialDocumentService
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class VisitCompletionService(
    private val visitRepository: VisitRepository,
    private val businessPolicy: VisitBusinessPolicy,
    private val financialDocumentService: FinancialDocumentService,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
    private val vehicleStatisticsCommandService: VehicleStatisticsCommandService,
    private val emailService: EmailSendingService,
) {
    private val logger = LoggerFactory.getLogger(VisitCompletionService::class.java)

    fun releaseVehicle(visitId: VisitId, request: ReleaseVehicleRequest, companyId: Long): Boolean {
        val visit = visitRepository.findById(visitId, companyId) 
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
        businessPolicy.validateReleaseConditions(visit)

        val documentItems = createDocumentItems(visit.services, request)
        val documentTotals = calculateDocumentTotals(documentItems)

        completeVisit(visit, companyId)
        createFinancialDocument(visitId, visit.title, request, companyId, documentItems, documentTotals)
        updateRevenueStatistics(visit.clientId, visit.vehicleId, companyId, visit.totalAmount())
        sendEmail(visitId)
        
        return true
    }

    private fun completeVisit(visit: Visit, companyId: Long) {
        val completedVisit = visit.copy(status = VisitStatus.COMPLETED)
        visitRepository.save(completedVisit)
    }

    private fun createDocumentItems(
        services: List<com.carslab.crm.production.modules.visits.domain.models.entities.VisitService>,
        request: ReleaseVehicleRequest
    ): List<DocumentItem> {
        return if (!request.overridenItems.isNullOrEmpty()) {
            createOverriddenItems(request.overridenItems)
        } else {
            createServiceItems(services)
        }
    }

    private fun createOverriddenItems(overriddenItems: List<CreateServiceCommand>): List<DocumentItem> {
        return overriddenItems.map { item ->
            val finalPrice = com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils.anyToBigDecimal(item.finalPrice ?: item.price)
            val quantity = com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils.anyToBigDecimal(item.quantity)
            val totalGross = finalPrice.multiply(quantity)
            val totalNet = com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils.calculateNetAmount(totalGross)

            DocumentItem(
                name = item.name,
                quantity = quantity,
                unitPrice = finalPrice,
                totalNet = totalNet,
                totalGross = totalGross
            )
        }
    }

    private fun createServiceItems(services: List<com.carslab.crm.production.modules.visits.domain.models.entities.VisitService>): List<DocumentItem> {
        return services.map { service ->
            val finalPrice = service.finalPrice
            val totalNet = com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils.calculateNetAmount(finalPrice)

            DocumentItem(
                name = service.name,
                quantity = BigDecimal.ONE,
                unitPrice = finalPrice,
                totalNet = totalNet,
                totalGross = finalPrice
            )
        }
    }

    private fun calculateDocumentTotals(items: List<DocumentItem>): DocumentTotals {
        val totalGross = items.sumOf { it.totalGross }
        val totalNet = items.sumOf { it.totalNet }
        val totalTax = com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils.calculateVatFromGrossNet(totalGross, totalNet)

        return DocumentTotals(totalGross, totalNet, totalTax)
    }

    private fun createFinancialDocument(
        visitId: VisitId,
        visitTitle: String,
        request: ReleaseVehicleRequest,
        companyId: Long,
        items: List<DocumentItem>,
        totals: DocumentTotals
    ) {
        try {
            val command = FinancialDocumentCommand(
                visitId = visitId.value.toString(),
                visitTitle = visitTitle,
                companyId = companyId,
                documentType = request.documentType,
                paymentDays = request.paymentDays.toInt(),
                paymentMethod = request.paymentMethod,
                additionalNotes = request.additionalNotes,
                items = items.map { item ->
                    FinancialDocumentItem(
                        name = item.name,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        totalNet = item.totalNet,
                        totalGross = item.totalGross
                    )
                },
                totals = FinancialDocumentTotals(
                    totalGross = totals.totalGross,
                    totalNet = totals.totalNet,
                    totalTax = totals.totalTax
                ),
                status = when(request.paymentMethod.uppercase()) {
                    "CASH" -> DocumentStatus.PAID
                    "CARD" -> DocumentStatus.PAID
                    else -> DocumentStatus.NOT_PAID
                }
            )

            financialDocumentService.createVisitDocument(command)
        } catch (e: Exception) {
            logger.error("Failed to create financial document for visit: ${visitId.value}", e)
            throw com.carslab.crm.production.shared.exception.BusinessException("Failed to create financial document")
        }
    }

    data class DocumentItem(
        val name: String,
        val quantity: BigDecimal,
        val unitPrice: BigDecimal,
        val totalNet: BigDecimal,
        val totalGross: BigDecimal
    )

    data class DocumentTotals(
        val totalGross: BigDecimal,
        val totalNet: BigDecimal,
        val totalTax: BigDecimal
    )

    data class FinancialDocumentCommand(
        val visitId: String,
        val visitTitle: String,
        val companyId: Long,
        val documentType: String,
        val paymentDays: Int,
        val paymentMethod: String,
        val additionalNotes: String?,
        val items: List<FinancialDocumentItem>,
        val totals: FinancialDocumentTotals,
        val status: DocumentStatus,
    )

    data class FinancialDocumentItem(
        val name: String,
        val quantity: BigDecimal,
        val unitPrice: BigDecimal,
        val totalNet: BigDecimal,
        val totalGross: BigDecimal
    )

    data class FinancialDocumentTotals(
        val totalGross: BigDecimal,
        val totalNet: BigDecimal,
        val totalTax: BigDecimal
    )
    
    private fun updateRevenueStatistics(clientId: ClientId, vehicleId: VehicleId, companyId: Long, totalAmount: BigDecimal) {
        try {
            clientStatisticsCommandService.updateTotalRevenue(clientId, totalAmount)
            vehicleStatisticsCommandService.addRevenue(vehicleId, totalAmount)
        } catch (e: Exception) {
            logger.error("Failed to update revenue statistics for client: $clientId or vehicle: $vehicleId", e)
        }
    }
    
    private fun sendEmail(visitId: VisitId) {
        emailService.sendProtocolEmail(visitId.value.toString())
    }
}