package com.carslab.crm.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InvoiceItemDTO(
    @JsonProperty("id")
    val id: String? = null,

    @field:NotBlank(message = "Nazwa pozycji jest wymagana")
    @JsonProperty("name")
    val name: String = "",

    @JsonProperty("description")
    val description: String? = null,

    @field:NotNull(message = "Ilość jest wymagana")
    @field:Positive(message = "Ilość musi być większa od zera")
    @JsonProperty("quantity")
    val quantity: BigDecimal = BigDecimal.ONE,

    @field:NotNull(message = "Cena jednostkowa jest wymagana")
    @field:Positive(message = "Cena jednostkowa musi być większa od zera")
    @JsonProperty("unit_price")
    val unitPrice: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Stawka VAT jest wymagana")
    @JsonProperty("tax_rate")
    val taxRate: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_net")
    val totalNet: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal = BigDecimal.ZERO
) {
    constructor() : this(
        name = "",
        quantity = BigDecimal.ONE,
        unitPrice = BigDecimal.ZERO,
        taxRate = BigDecimal.ZERO
    )
}

data class InvoiceAttachmentDTO(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("url")
    val url: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("uploaded_at")
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

data class InvoiceFilterDTO(
    @JsonProperty("number")
    val number: String? = null,

    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("buyer_name")
    val buyerName: String? = null,

    @JsonProperty("status")
    val status: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_from")
    val dateFrom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_to")
    val dateTo: LocalDate? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("min_amount")
    val minAmount: BigDecimal? = null,

    @JsonProperty("max_amount")
    val maxAmount: BigDecimal? = null
)

data class ExtractedInvoiceDataDTO(
    @JsonProperty("general_info")
    val generalInfo: GeneralInfoDTO,

    @JsonProperty("seller")
    val seller: SellerInfoDTO,

    @JsonProperty("buyer")
    val buyer: BuyerInfoDTO,

    @JsonProperty("items")
    val items: List<ExtractedItemDTO>,

    @JsonProperty("summary")
    val summary: SummaryDTO,

    @JsonProperty("notes")
    val notes: String? = null
)

data class GeneralInfoDTO(
    @JsonProperty("title")
    val title: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("issued_date")
    val issuedDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_date")
    val dueDate: LocalDate
)

data class SellerInfoDTO(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("address")
    val address: String? = null
)

data class BuyerInfoDTO(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("tax_id")
    val taxId: String? = null,

    @JsonProperty("address")
    val address: String? = null
)

data class ExtractedItemDTO(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("quantity")
    val quantity: BigDecimal,

    @JsonProperty("unit_price")
    val unitPrice: BigDecimal,

    @JsonProperty("tax_rate")
    val taxRate: BigDecimal,

    @JsonProperty("total_net")
    val totalNet: BigDecimal,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal
)

data class SummaryDTO(
    @JsonProperty("total_net")
    val totalNet: BigDecimal,

    @JsonProperty("total_tax")
    val totalTax: BigDecimal,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal
)

data class InvoiceDataResponse(
    @JsonProperty("extracted_invoice_data")
    val extractedInvoiceData: ExtractedInvoiceDataDTO
)