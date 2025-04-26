package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

/**
 * DTO reprezentujące odpowiedź z danymi statystyk gotówkowych.
 */
data class CashStatisticsResponse(
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("period_start")
    val periodStart: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("period_end")
    val periodEnd: LocalDate,

    @JsonProperty("income")
    val income: BigDecimal,

    @JsonProperty("expense")
    val expense: BigDecimal,

    @JsonProperty("balance")
    val balance: BigDecimal,

    @JsonProperty("transaction_count")
    val transactionCount: Int
)