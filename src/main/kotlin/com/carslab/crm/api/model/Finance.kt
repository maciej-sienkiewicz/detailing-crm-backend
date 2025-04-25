package com.carslab.crm.api.model

import com.carslab.crm.domain.model.view.finance.*
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DTO pozycji faktury.
 */
data class InvoiceItemDTO(
    val id: String? = null,

    @field:NotBlank(message = "Nazwa pozycji jest wymagana")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Ilość jest wymagana")
    @field:Positive(message = "Ilość musi być większa od zera")
    val quantity: BigDecimal,

    @field:NotNull(message = "Cena jednostkowa jest wymagana")
    @field:Positive(message = "Cena jednostkowa musi być większa od zera")
    val unitPrice: BigDecimal,

    @field:NotNull(message = "Stawka VAT jest wymagana")
    val taxRate: BigDecimal,

    val totalNet: BigDecimal,
    val totalGross: BigDecimal
)

/**
 * DTO załącznika faktury.
 */
data class InvoiceAttachmentDTO(
    val id: String? = null,
    val name: String,
    val size: Long,
    val type: String,
    val url: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * DTO faktury (odpowiedź).
 */
data class InvoiceResponse(
    val id: String,
    val number: String,
    val title: String,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val issuedDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val dueDate: LocalDate,

    val sellerName: String,
    val sellerTaxId: String? = null,
    val sellerAddress: String? = null,
    val buyerName: String,
    val buyerTaxId: String? = null,
    val buyerAddress: String? = null,
    val clientId: Long? = null,
    val status: String,
    val type: String,
    val paymentMethod: String,
    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,
    val currency: String,
    val notes: String? = null,
    val protocolId: String? = null,
    val protocolNumber: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime,

    val items: List<InvoiceItemDTO>,
    val attachment: InvoiceAttachmentDTO? = null
)

/**
 * DTO faktury (żądanie utworzenia).
 */
data class CreateInvoiceRequest(
    @field:NotBlank(message = "Tytuł faktury jest wymagany")
    val title: String,

    @field:NotNull(message = "Data wystawienia jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val issuedDate: LocalDate,

    @field:NotNull(message = "Termin płatności jest wymagany")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dueDate: LocalDate,

    @field:NotBlank(message = "Nazwa sprzedawcy jest wymagana")
    val sellerName: String,

    val sellerTaxId: String? = null,
    val sellerAddress: String? = null,

    @field:NotBlank(message = "Nazwa nabywcy jest wymagana")
    val buyerName: String,

    val buyerTaxId: String? = null,
    val buyerAddress: String? = null,
    val clientId: Long? = null,

    @field:NotNull(message = "Status faktury jest wymagany")
    val status: String,

    @field:NotNull(message = "Typ faktury jest wymagany")
    val type: String,

    @field:NotNull(message = "Metoda płatności jest wymagana")
    val paymentMethod: String,

    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,

    @field:NotBlank(message = "Waluta jest wymagana")
    val currency: String,

    val paid: BigDecimal? = null,
    val notes: String? = null,
    val protocolId: String? = null,
    val protocolNumber: String? = null,

    @field:NotNull(message = "Lista pozycji jest wymagana")
    @field:Size(min = 1, message = "Faktura musi zawierać co najmniej jedną pozycję")
    @field:Valid
    val items: List<InvoiceItemDTO>
)

/**
 * DTO faktury (żądanie aktualizacji).
 */
data class UpdateInvoiceRequest(
    @field:NotBlank(message = "Tytuł faktury jest wymagany")
    val title: String,

    @field:NotNull(message = "Data wystawienia jest wymagana")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val issuedDate: LocalDate,

    @field:NotNull(message = "Termin płatności jest wymagany")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dueDate: LocalDate,

    @field:NotBlank(message = "Nazwa sprzedawcy jest wymagana")
    val sellerName: String,

    val sellerTaxId: String? = null,
    val sellerAddress: String? = null,

    @field:NotBlank(message = "Nazwa nabywcy jest wymagana")
    val buyerName: String,

    val buyerTaxId: String? = null,
    val buyerAddress: String? = null,
    val clientId: Long? = null,

    @field:NotNull(message = "Status faktury jest wymagany")
    val status: String,

    @field:NotNull(message = "Typ faktury jest wymagany")
    val type: String,

    @field:NotNull(message = "Metoda płatności jest wymagana")
    val paymentMethod: String,

    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal,

    @field:NotBlank(message = "Waluta jest wymagana")
    val currency: String,

    val paid: BigDecimal? = null,
    val notes: String? = null,
    val protocolId: String? = null,
    val protocolNumber: String? = null,

    @field:NotNull(message = "Lista pozycji jest wymagana")
    @field:Size(min = 1, message = "Faktura musi zawierać co najmniej jedną pozycję")
    @field:Valid
    val items: List<InvoiceItemDTO>
)

/**
 * DTO dla filtrów wyszukiwania faktur.
 */
data class InvoiceFilterDTO(
    val number: String? = null,
    val title: String? = null,
    val buyerName: String? = null,
    val status: String? = null,
    val type: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val dateFrom: LocalDate? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val dateTo: LocalDate? = null,

    val protocolId: String? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null
)

/**
 * DTO dla danych wyekstrahowanych z faktury.
 */
data class ExtractedInvoiceDataDTO(
    val generalInfo: GeneralInfoDTO,
    val seller: SellerInfoDTO,
    val buyer: BuyerInfoDTO,
    val items: List<ExtractedItemDTO>,
    val summary: SummaryDTO,
    val notes: String? = null
)

data class GeneralInfoDTO(
    val title: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val issuedDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val dueDate: LocalDate
)

data class SellerInfoDTO(
    val name: String,
    val taxId: String? = null,
    val address: String? = null
)

data class BuyerInfoDTO(
    val name: String,
    val taxId: String? = null,
    val address: String? = null
)

data class ExtractedItemDTO(
    val name: String,
    val description: String? = null,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxRate: BigDecimal,
    val totalNet: BigDecimal,
    val totalGross: BigDecimal
)

data class SummaryDTO(
    val totalNet: BigDecimal,
    val totalTax: BigDecimal,
    val totalGross: BigDecimal
)

/**
 * DTO dla odpowiedzi z danymi wyekstrahowanymi z faktury.
 */
data class InvoiceDataResponse(
    val extractedInvoiceData: ExtractedInvoiceDataDTO
)