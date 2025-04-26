package com.carslab.crm.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

/**
 * DTO dla filtrowania transakcji gotówkowych.
 */
data class CashTransactionFilterDTO(
    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_from")
    val dateFrom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_to")
    val dateTo: LocalDate? = null,

    @JsonProperty("visit_id")
    val visitId: String? = null,

    @JsonProperty("min_amount")
    val minAmount: BigDecimal? = null,

    @JsonProperty("max_amount")
    val maxAmount: BigDecimal? = null
)