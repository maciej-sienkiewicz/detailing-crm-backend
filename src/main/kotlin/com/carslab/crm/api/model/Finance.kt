package com.carslab.crm.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

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