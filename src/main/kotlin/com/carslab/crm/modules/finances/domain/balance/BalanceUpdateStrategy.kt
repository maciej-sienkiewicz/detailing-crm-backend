package com.carslab.crm.modules.finances.domain.balance

import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import java.math.BigDecimal

interface BalanceUpdateStrategy {
    fun canHandle(paymentMethod: PaymentMethod): Boolean
    fun updateBalance(context: BalanceUpdateContext, updateReason: String)
}

data class BalanceUpdateContext(
    val companyId: Long,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val timestamp: String,
    val documentId: String,
    val description: String? = null
)


