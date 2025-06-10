package com.carslab.crm.modules.finances.api

import java.math.BigDecimal

data class BalanceOverrideResult(
    val success: Boolean,
    val operationId: Long?,
    val previousBalance: BigDecimal?,
    val newBalance: BigDecimal,
    val difference: BigDecimal?,
    val message: String,
    val error: Throwable? = null,
    val pendingApprovalId: String? = null
)