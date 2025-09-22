package com.carslab.crm.production.modules.visits.domain.service.aggregate

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.modules.email.domain.services.EmailSendingService
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.application.service.VehicleStatisticsCommandService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommentCommandService
import com.carslab.crm.production.modules.visits.domain.activity.VisitActivitySender
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.policy.VisitBusinessPolicy
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import com.carslab.crm.production.modules.visits.domain.service.details.FinancialDocumentService
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val activitySender: VisitActivitySender,
    private val commentCommandService: VisitCommentCommandService,
    private val backgroundTaskScope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(VisitCompletionService::class.java)

    fun releaseVehicle(visitId: VisitId, request: ReleaseVehicleRequest, authContext: AuthContext): Boolean {
        val visit = visitRepository.findById(visitId, authContext.companyId.value, authContext) 
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
        businessPolicy.validateReleaseConditions(visit)

        val documentItems = createDocumentItems(visit.services, request)
        val documentTotals = calculateDocumentTotals(documentItems)
        
        completeVisit(visit, authContext.companyId.value)
        
        activitySender.onVisitCompleted(visit, authContext)

        backgroundTaskScope.launch {
            launch {
                runCatching { createFinancialDocument(visitId, visit.title, request, authContext.companyId.value, documentItems, documentTotals, authContext) }
                    .onFailure {
                        commentCommandService.addWarning(
                            AddCommentRequest(
                                content = "Nie udało się wygenerować dokumentu finansowego. Upewnij się, że masz aktywny i pooprawny szablon faktury.",
                                type = "SYSTEM",
                                visitId = visit.id!!.value.toString()
                            ),
                            companyId = authContext.companyId.value
                        )
                        logger.error("Failed to update revenue statistics", it) 
                    }
            }
            
            launch {
                runCatching { updateRevenueStatistics(visit.clientId, visit.vehicleId, authContext.companyId.value, visit.totalAmount()) }
                    .onFailure { logger.error("Failed to update revenue statistics", it) }
            }

            launch {
                runCatching { sendEmail(visitId, authContext) }
                    .onFailure { 
                        commentCommandService.addWarning(
                            AddCommentRequest(
                                content = "Nie udało się wysłać e-maila z protokołem. Upewnij się, że masz poprawną konfigurację maila. ",
                                type = "SYSTEM",
                                visitId = visit.id!!.value.toString(),
                            ),
                            companyId = authContext.companyId.value
                        )
                        logger.error("Failed to send email", it)
                    }
            }

            launch {
                runCatching { addCommentsToVisit(request, visit.id!!) }
                    .onFailure { logger.error("Failed to add comments", it) }
            }
        }
        
        return true
    }

    private fun addCommentsToVisit(
        request: ReleaseVehicleRequest,
        visitId: VisitId
    ) {
        commentCommandService.addComment(AddCommentRequest(
                content = "Płatność: ${humanFriendyPaymentMethod(request.paymentMethod)}. Dokument: ${humanFriendyDocumentType(request.documentType)}",
                type = "SYSTEM",
                visitId = visitId.value.toString()
            ))
    }
    
    private fun humanFriendyPaymentMethod(method: String): String {
        return when(method.uppercase()) {
            "CASH" -> "Gotówka"
            "CARD" -> "Karta"
            "TRANSFER" -> "Przelew"
            else -> method
        }
    }
    
    private fun humanFriendyDocumentType(type: String): String {
        return when(type.uppercase()) {
            "INVOICE" -> "Faktura"
            "RECEIPT" -> "Paragon"
            "PROFORMA" -> "Proforma"
            else -> type
        }
    }

    private fun completeVisit(visit: Visit, companyId: Long) {
        val completedVisit = visit.copy(status = VisitStatus.COMPLETED)
        visitRepository.save(completedVisit)
    }

    private fun createDocumentItems(
        services: List<VisitService>,
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
            val finalPrice = CalculationUtils.anyToBigDecimal(item.finalPrice ?: item.price)
            val quantity = CalculationUtils.anyToBigDecimal(item.quantity)
            val totalGross = finalPrice.multiply(quantity)
            val totalNet = CalculationUtils.calculateNetAmount(totalGross)

            DocumentItem(
                name = item.name,
                quantity = quantity,
                unitPrice = finalPrice,
                totalNet = totalNet,
                totalGross = totalGross
            )
        }
    }

    private fun createServiceItems(services: List<VisitService>): List<DocumentItem> {
        return services.map { service ->
            val finalPrice = service.finalPrice
            val totalNet = CalculationUtils.calculateNetAmount(finalPrice)

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
        val totalTax = CalculationUtils.calculateVatFromGrossNet(totalGross, totalNet)

        return DocumentTotals(totalGross, totalNet, totalTax)
    }

    private fun createFinancialDocument(
        visitId: VisitId,
        visitTitle: String,
        request: ReleaseVehicleRequest,
        companyId: Long,
        items: List<DocumentItem>,
        totals: DocumentTotals,
        authContext: AuthContext,
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

            financialDocumentService.createVisitDocument(command, authContext)
        } catch (e: Exception) {
            logger.error("Failed to create financial document for visit: ${visitId.value}", e)
            throw BusinessException("Failed to create financial document")
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
    
    private fun sendEmail(visitId: VisitId, authContext: AuthContext) {
        emailService.sendProtocolEmail(visitId.value.toString(), authContext = authContext)
    }
}