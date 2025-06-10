package com.carslab.crm.modules.finances.api

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import com.carslab.crm.modules.finances.infrastructure.entity.OverrideReason
import java.math.BigDecimal

data class BalanceOverrideRequest(
    val companyId: Long,
    val balanceType: BalanceType,
    val newBalance: BigDecimal,
    val reason: OverrideReason,
    val userId: String,
    val description: String? = null,
    val isPreApproved: Boolean = false,
    val approvedBy: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

data class CashMoveRequest(
    val amount: BigDecimal,
    val description: String? = null
)

data class BankReconciliationRequest(
    val statementBalance: BigDecimal,
    val description: String
)

data class CashInventoryRequest(
    val countedAmount: BigDecimal,
    val notes: String
)

data class ManualOverrideRequest(
    val balanceType: BalanceType,
    val newBalance: BigDecimal,
    val reason: OverrideReason,
    val description: String? = null
)