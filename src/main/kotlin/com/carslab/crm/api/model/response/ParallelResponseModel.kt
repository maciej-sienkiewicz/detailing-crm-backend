package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * Models for parallel processing of invoice data
 */

/**
 * Response model for the headers request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HeadersResponse(
    @JsonProperty("extractedInvoiceHeaders")
    val extractedInvoiceHeaders: ExtractedInvoiceHeaders
)

/**
 * Header information extracted from invoice
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExtractedInvoiceHeaders(
    @JsonProperty("generalInfo")
    val generalInfo: GeneralInfo,

    @JsonProperty("seller")
    val seller: Entity,

    @JsonProperty("buyer")
    val buyer: Entity
)

/**
 * Response model for the items request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ItemsResponse(
    @JsonProperty("extractedInvoiceItems")
    val extractedInvoiceItems: ExtractedInvoiceItems
)

/**
 * Items and summary information extracted from invoice
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExtractedInvoiceItems(
    @JsonProperty("items")
    val items: List<InvoiceItem>,

    @JsonProperty("summary")
    val summary: InvoiceSummary,

    @JsonProperty("additionalInfo")
    val additionalInfo: AdditionalInfo? = null
)