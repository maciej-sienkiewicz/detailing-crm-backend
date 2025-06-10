package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * Response model for invoice data extraction
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceDataResponse(
    @JsonProperty("extractedInvoiceData")
    val extractedDocumentData: ExtractedInvoiceData
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceDataResponseWithDirection(
    @JsonProperty("extractedInvoiceData")
    val extractedDocumentData: ExtractedInvoiceData,
    @JsonProperty("direction")
    val direction: String
)

/**
 * Main container for extracted invoice data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExtractedInvoiceData(
    @JsonProperty("generalInfo")
    val generalInfo: GeneralInfo,

    @JsonProperty("seller")
    val seller: Entity,

    @JsonProperty("buyer")
    val buyer: Entity,

    @JsonProperty("items")
    val items: List<InvoiceItem>,

    @JsonProperty("summary")
    val summary: InvoiceSummary,

    @JsonProperty("notes")
    val notes: String? = null
)

/**
 * General invoice information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeneralInfo(
    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("issuedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val issuedDate: LocalDate,

    @JsonProperty("dueDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val dueDate: LocalDate
)

/**
 * Entity details (seller or buyer)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Entity(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("taxId")
    val taxId: String? = null,

    @JsonProperty("address")
    val address: String? = null
)

/**
 * Invoice line item details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceItem(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("quantity")
    val quantity: Double,

    @JsonProperty("unitPrice")
    val unitPrice: Double,

    @JsonProperty("taxRate")
    val taxRate: Double,

    @JsonProperty("totalNet")
    val totalNet: Double,

    @JsonProperty("totalGross")
    val totalGross: Double
)

/**
 * Invoice summary (totals)
 */
data class InvoiceSummary(
    @JsonProperty("totalNet")
    val totalNet: Double,

    @JsonProperty("totalTax")
    val totalTax: Double,

    @JsonProperty("totalGross")
    val totalGross: Double
)

/**
 * Additional invoice information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdditionalInfo(
    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("protocolNumber")
    val protocolNumber: String? = null,

    @JsonProperty("protocolId")
    val protocolId: String? = null
)