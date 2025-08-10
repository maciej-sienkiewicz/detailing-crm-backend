package com.carslab.crm.modules.finances.api

import com.carslab.crm.modules.finances.infrastructure.entity.BalanceType
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class BalanceOverrideRequest(
    val companyId: Long,
    @JsonProperty("balance_type")
    val balanceType: BalanceType,
    @JsonProperty("new_balance")
    val newBalance: BigDecimal,
    val description: String,
    val userId: String,
    val isPreApproved: Boolean = false,
    val approvedBy: String? = null,
    val ipAddress: String? = null
)

data class CashMoveRequest(
    @field:NotNull
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String
)

data class BankReconciliationRequest(
    @field:NotNull
    @field:DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @JsonProperty("statement_balance")
    val statementBalance: BigDecimal,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String
)

data class CashInventoryRequest(
    @field:NotNull
    @field:DecimalMin(value = "0.00", message = "Counted amount cannot be negative")
    @JsonProperty("counted_amount")
    val countedAmount: BigDecimal,

    @field:NotBlank(message = "Notes are required for inventory")
    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    val notes: String
)

data class ManualBalanceOverrideRequest(
    @field:NotNull
    @JsonProperty("balance_type")
    val balanceType: BalanceType,

    @field:NotNull
    @field:DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @JsonProperty("new_balance")
    val newBalance: BigDecimal,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String
)