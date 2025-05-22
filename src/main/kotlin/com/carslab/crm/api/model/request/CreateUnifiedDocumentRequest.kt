// src/main/kotlin/com/carslab/crm/api/model/request/CreateUnifiedDocumentRequest.kt
package com.carslab.crm.api.model.request

import com.carslab.crm.api.model.DocumentItemDTO
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateUnifiedDocumentRequest(
    @field:NotBlank(message = "Typ dokumentu jest wymagany")
    @JsonProperty("type")
    val type: String = "INVOICE",

    @field:NotBlank(message = "Tytuł dokumentu jest wymagany")
    @JsonProperty("title")
    val title: String = "",

    @JsonProperty("description")
    val description: String? = null,

    @field:NotNull(message = "Data wystawienia jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("issued_date")
    val issuedDate: LocalDate = LocalDate.now(),

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_date")
    val dueDate: LocalDate? = null,

    @field:NotBlank(message = "Nazwa sprzedawcy jest wymagana")
    @JsonProperty("seller_name")
    val sellerName: String = "",

    @JsonProperty("seller_tax_id")
    val sellerTaxId: String? = null,

    @JsonProperty("seller_address")
    val sellerAddress: String? = null,

    @field:NotBlank(message = "Nazwa nabywcy jest wymagana")
    @JsonProperty("buyer_name")
    val buyerName: String = "",

    @JsonProperty("buyer_tax_id")
    val buyerTaxId: String? = null,

    @JsonProperty("buyer_address")
    val buyerAddress: String? = null,

    @field:NotNull(message = "Status dokumentu jest wymagany")
    @JsonProperty("status")
    val status: String = "NOT_PAID",

    @field:NotNull(message = "Kierunek transakcji jest wymagany")
    @JsonProperty("direction")
    val direction: String = "INCOME",

    @field:NotNull(message = "Metoda płatności jest wymagana")
    @JsonProperty("payment_method")
    val paymentMethod: String = "BANK_TRANSFER",

    @JsonProperty("total_net")
    val totalNet: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_tax")
    val totalTax: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("paid_amount")
    val paidAmount: BigDecimal? = null,

    @field:NotBlank(message = "Waluta jest wymagana")
    @JsonProperty("currency")
    val currency: String = "PLN",

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("protocol_number")
    val protocolNumber: String? = null,

    @JsonProperty("visit_id")
    val visitId: String? = null,

    @field:Valid
    @JsonProperty("items")
    val items: List<DocumentItemDTO> = emptyList()
)

// src/main/kotlin/com/carslab/crm/api/model/request/UpdateUnifiedDocumentRequest.kt
data class UpdateUnifiedDocumentRequest(
    @field:NotBlank(message = "Typ dokumentu jest wymagany")
    @JsonProperty("type")
    val type: String,

    @field:NotBlank(message = "Tytuł dokumentu jest wymagany")
    @JsonProperty("title")
    val title: String,

    @JsonProperty("description")
    val description: String? = null,

    @field:NotNull(message = "Data wystawienia jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("issued_date")
    val issuedDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_date")
    val dueDate: LocalDate? = null,

    @field:NotBlank(message = "Nazwa sprzedawcy jest wymagana")
    @JsonProperty("seller_name")
    val sellerName: String,

    @JsonProperty("seller_tax_id")
    val sellerTaxId: String? = null,

    @JsonProperty("seller_address")
    val sellerAddress: String? = null,

    @field:NotBlank(message = "Nazwa nabywcy jest wymagana")
    @JsonProperty("buyer_name")
    val buyerName: String,

    @JsonProperty("buyer_tax_id")
    val buyerTaxId: String? = null,

    @JsonProperty("buyer_address")
    val buyerAddress: String? = null,

    @field:NotNull(message = "Status dokumentu jest wymagany")
    @JsonProperty("status")
    val status: String,

    @field:NotNull(message = "Kierunek transakcji jest wymagany")
    @JsonProperty("direction")
    val direction: String,

    @field:NotNull(message = "Metoda płatności jest wymagana")
    @JsonProperty("payment_method")
    val paymentMethod: String,

    @JsonProperty("total_net")
    val totalNet: BigDecimal,

    @JsonProperty("total_tax")
    val totalTax: BigDecimal,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal,

    @JsonProperty("paid_amount")
    val paidAmount: BigDecimal? = null,

    @field:NotBlank(message = "Waluta jest wymagana")
    @JsonProperty("currency")
    val currency: String,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("protocol_number")
    val protocolNumber: String? = null,

    @JsonProperty("visit_id")
    val visitId: String? = null,

    @field:Valid
    @JsonProperty("items")
    val items: List<DocumentItemDTO>
)