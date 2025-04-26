package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DTO reprezentujące odpowiedź z danymi transakcji gotówkowej.
 */
data class CashTransactionResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("description")
    val description: String,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date")
    val date: LocalDate,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("visit_id")
    val visitId: String? = null,

    @JsonProperty("created_by")
    val createdBy: String,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)