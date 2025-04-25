package com.carslab.crm.api.model.request

import com.carslab.crm.api.model.InvoiceItemDTO
import com.carslab.crm.domain.model.view.finance.InvoiceStatus
import com.carslab.crm.domain.model.view.finance.InvoiceType
import com.carslab.crm.domain.model.view.finance.PaymentMethod
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateInvoiceRequest(
    @field:NotBlank(message = "Tytuł faktury jest wymagany")
    @JsonProperty("title")
    val title: String = "",

    @field:NotNull(message = "Data wystawienia jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("issued_date")
    val issuedDate: LocalDate = LocalDate.now(),

    @field:NotNull(message = "Termin płatności jest wymagany")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_date")
    val dueDate: LocalDate = LocalDate.now(),

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

    @JsonProperty("client_id")
    val clientId: Long? = null,

    @field:NotNull(message = "Status faktury jest wymagany")
    @JsonProperty("status")
    val status: String = InvoiceStatus.DRAFT.name,

    @field:NotNull(message = "Typ faktury jest wymagany")
    @JsonProperty("type")
    val type: String = InvoiceType.INCOME.name,

    @field:NotNull(message = "Metoda płatności jest wymagana")
    @JsonProperty("payment_method")
    val paymentMethod: String = PaymentMethod.BANK_TRANSFER.name,

    @JsonProperty("total_net")
    val totalNet: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_tax")
    val totalTax: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank(message = "Waluta jest wymagana")
    @JsonProperty("currency")
    val currency: String = "PLN",

    @JsonProperty("paid")
    val paid: BigDecimal? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("protocol_number")
    val protocolNumber: String? = null,

    @field:NotNull(message = "Lista pozycji jest wymagana")
    @field:Size(min = 1, message = "Faktura musi zawierać co najmniej jedną pozycję")
    @field:Valid
    @JsonProperty("items")
    val items: List<InvoiceItemDTO> = emptyList()
) {
    // Adding an explicit no-args constructor for Jackson
    constructor() : this(
        title = "",
        issuedDate = LocalDate.now(),
        dueDate = LocalDate.now(),
        sellerName = "",
        buyerName = "",
        items = emptyList()
    )
}