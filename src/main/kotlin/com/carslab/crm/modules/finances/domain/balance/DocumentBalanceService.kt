package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationType
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

interface DocumentBalanceService {
    fun handleDocumentChange(document: UnifiedFinancialDocument, oldStatus: DocumentStatus?, companyId: Long)
    fun handleDocumentDeletion(document: UnifiedFinancialDocument, companyId: Long)
}

@Service
@Transactional
class DocumentBalanceServiceImpl(
    private val balanceService: BalanceService
) : DocumentBalanceService {

    private val logger = LoggerFactory.getLogger(DocumentBalanceServiceImpl::class.java)

    override fun handleDocumentChange(
        document: UnifiedFinancialDocument,
        oldStatus: DocumentStatus?,
        companyId: Long
    ) {
        val newStatus = document.status

        logger.debug("Handling document status change: ${document.id.value} from $oldStatus to $newStatus")

        if (oldStatus != null && oldStatus != newStatus) {
            reverseBalanceForStatus(document, oldStatus, companyId)
        }

        applyBalanceForStatus(document, newStatus, companyId)
    }

    override fun handleDocumentDeletion(document: UnifiedFinancialDocument, companyId: Long) {
        logger.debug("Handling document deletion: ${document.id.value} with status ${document.status}")
        reverseBalanceForStatus(document, document.status, companyId)
    }

    private fun applyBalanceForStatus(
        document: UnifiedFinancialDocument,
        status: DocumentStatus,
        companyId: Long,
    ) {
        if (!shouldAffectBalance(status)) return

        val amount = getEffectiveAmount(document, status)
        if (amount == BigDecimal.ZERO) return

        val operation = getBalanceOperation(document.direction, true)
        val balanceType = getBalanceType(document.paymentMethod)

        try {
            balanceService.updateBalance(
                companyId = companyId,
                balanceType = balanceType,
                amount = amount,
                operation = operation,
                documentId = document.id.value,
                updateReason = "Dokument o tytule: ${document.title} zmienił status na ${humanFriendlyStatus(status)}"
            )
        } catch (e: Exception) {
            logger.error("Failed to apply balance for document ${document.id.value}: ${e.message}", e)
        }
    }
    
    private fun humanFriendlyStatus(status: DocumentStatus): String {
        return when (status) {
            DocumentStatus.NOT_PAID -> "Wysłany"
            DocumentStatus.PAID -> "Opłacony"
            DocumentStatus.CANCELLED -> "Anulowany"
            DocumentStatus.PARTIALLY_PAID -> "Częściowo opłacony"
            DocumentStatus.OVERDUE -> "Przeterminowany"
        }
    }

    private fun reverseBalanceForStatus(
        document: UnifiedFinancialDocument,
        status: DocumentStatus,
        companyId: Long
    ) {
        if (!shouldAffectBalance(status)) return

        val amount = getEffectiveAmount(document, status)
        if (amount == BigDecimal.ZERO) return

        val operation = getBalanceOperation(document.direction, false)
        val balanceType = getBalanceType(document.paymentMethod)

        try {
            balanceService.updateBalance(
                companyId = companyId,
                balanceType = balanceType,
                amount = amount,
                operation = operation,
                documentId = document.id.value,
                updateReason = "Dokument o tytule: ${document.title} zmienił status na $status"
            )
        } catch (e: Exception) {
            logger.error("Failed to reverse balance for document ${document.id.value}: ${e.message}", e)
        }
    }

    private fun shouldAffectBalance(status: DocumentStatus): Boolean {
        return status in listOf(DocumentStatus.PAID, DocumentStatus.PARTIALLY_PAID)
    }

    private fun getEffectiveAmount(document: UnifiedFinancialDocument, status: DocumentStatus): BigDecimal {
        return when (status) {
            DocumentStatus.PAID -> document.totalGross
            DocumentStatus.PARTIALLY_PAID -> document.paidAmount
            else -> BigDecimal.ZERO
        }
    }

    private fun getBalanceOperation(direction: TransactionDirection, isApply: Boolean): BalanceOperationType {
        return when {
            direction == TransactionDirection.INCOME && isApply -> BalanceOperationType.ADD
            direction == TransactionDirection.INCOME && !isApply -> BalanceOperationType.SUBTRACT
            direction == TransactionDirection.EXPENSE && isApply -> BalanceOperationType.SUBTRACT
            direction == TransactionDirection.EXPENSE && !isApply -> BalanceOperationType.ADD
            else -> throw IllegalArgumentException("Invalid direction: $direction")
        }
    }

    private fun getBalanceType(paymentMethod: PaymentMethod): BalanceType {
        return when (paymentMethod) {
            PaymentMethod.CASH -> BalanceType.CASH
            PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD -> BalanceType.BANK
            else -> BalanceType.BANK
        }
    }
}