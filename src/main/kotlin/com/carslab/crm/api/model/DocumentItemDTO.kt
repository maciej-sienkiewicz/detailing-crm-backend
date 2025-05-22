package com.carslab.crm.api.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DocumentItemDTO(
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
)

data class DocumentAttachmentDTO(
    @JsonProperty("id")
    val id: String,

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
    val uploadedAt: LocalDateTime
)

data class UnifiedDocumentFilterDTO(
    @JsonProperty("number")
    val number: String? = null,

    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("buyer_name")
    val buyerName: String? = null,

    @JsonProperty("seller_name")
    val sellerName: String? = null,

    @JsonProperty("status")
    val status: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("direction")
    val direction: String? = null,

    @JsonProperty("payment_method")
    val paymentMethod: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_from")
    val dateFrom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("date_to")
    val dateTo: LocalDate? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("visit_id")
    val visitId: String? = null,

    @JsonProperty("min_amount")
    val minAmount: BigDecimal? = null,

    @JsonProperty("max_amount")
    val maxAmount: BigDecimal? = null
)

data class ExtractedDocumentDataDTO(
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

data class DocumentDataResponse(
    @JsonProperty("extracted_document_data")
    val extractedDocumentData: ExtractedDocumentDataDTO
)

// Enum dla typu dokumentu
enum class DocumentType {
    INVOICE,    // Faktura
    RECEIPT,    // Paragon
    OTHER       // Inna operacja
}

// Enum dla kierunku transakcji
enum class TransactionDirection {
    INCOME,     // Przychód
    EXPENSE     // Wydatek
}

// Enum dla statusu dokumentu
enum class DocumentStatus {
    NOT_PAID,         // Nieopłacone
    PAID,             // Opłacone
    PARTIALLY_PAID,   // Częściowo opłacone
    OVERDUE,          // Przeterminowane
    CANCELLED         // Anulowane
}

data class FinancialSummaryResponse(
    @JsonProperty("cash_balance")
    val cashBalance: BigDecimal,

    @JsonProperty("total_income")
    val totalIncome: BigDecimal,

    @JsonProperty("total_expense")
    val totalExpense: BigDecimal,

    @JsonProperty("bank_account_balance")
    val bankAccountBalance: BigDecimal,

    @JsonProperty("receivables")
    val receivables: BigDecimal,

    @JsonProperty("receivables_overdue")
    val receivablesOverdue: BigDecimal,

    @JsonProperty("liabilities")
    val liabilities: BigDecimal,

    @JsonProperty("liabilities_overdue")
    val liabilitiesOverdue: BigDecimal,

    @JsonProperty("profit")
    val profit: BigDecimal,

    @JsonProperty("cash_flow")
    val cashFlow: BigDecimal,

    @JsonProperty("income_by_method")
    val incomeByMethod: Map<String, BigDecimal>,

    @JsonProperty("expense_by_method")
    val expenseByMethod: Map<String, BigDecimal>,

    @JsonProperty("receivables_by_timeframe")
    val receivablesByTimeframe: Map<String, BigDecimal>,

    @JsonProperty("liabilities_by_timeframe")
    val liabilitiesByTimeframe: Map<String, BigDecimal>
)