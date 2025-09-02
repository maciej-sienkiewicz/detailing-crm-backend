package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceOperationType
import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import org.springframework.stereotype.Component

@Component
class BankBalanceStrategy(
    private val balanceService: BalanceService
) : BalanceUpdateStrategy {

    override fun canHandle(paymentMethod: PaymentMethod): Boolean {
        return paymentMethod in listOf(PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD)
    }

    override fun updateBalance(context: BalanceUpdateContext, updateReason: String) {
        val operation = when (context.direction) {
            TransactionDirection.INCOME -> BalanceOperationType.ADD
            TransactionDirection.EXPENSE -> BalanceOperationType.SUBTRACT
        }

        balanceService.updateBalance(
            companyId = context.companyId,
            balanceType = BalanceType.BANK,
            amount = context.amount,
            operation = operation,
            documentId = context.documentId,
            updateReason = updateReason
        )
    }
}