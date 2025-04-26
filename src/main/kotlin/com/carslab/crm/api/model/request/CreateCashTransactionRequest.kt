package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

/**
 * DTO dla żądania utworzenia nowej transakcji gotówkowej.
 */
data class CreateCashTransactionRequest(
    @field:NotBlank(message = "Typ transakcji jest wymagany")
    @JsonProperty("type")
    val type: String,

    @field:NotBlank(message = "Opis transakcji jest wymagany")
    @JsonProperty("description")
    val description: String,

    @field:NotNull(message = "Data transakcji jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date")
    val date: LocalDate = LocalDate.now(),

    @field:NotNull(message = "Kwota jest wymagana")
    @field:Positive(message = "Kwota musi być większa od zera")
    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("visit_id")
    val visitId: String? = null,
) {
    // Konstruktor bezargumentowy dla Jacksona
    constructor() : this(
        type = "INCOME",
        description = "",
        date = LocalDate.now(),
        amount = BigDecimal.ONE
    )
}