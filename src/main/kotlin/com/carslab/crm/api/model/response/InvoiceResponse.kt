package com.carslab.crm.api.model.response

import com.carslab.crm.api.model.InvoiceAttachmentDTO
import com.carslab.crm.api.model.InvoiceItemDTO
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InvoiceResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("number")
    val number: String,

    @JsonProperty("title")
    val title: String,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("issued_date")
    val issuedDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("due_date")
    val dueDate: LocalDate,

    @JsonProperty("seller_name")
    val sellerName: String,

    @JsonProperty("seller_tax_id")
    val sellerTaxId: String? = null,

    @JsonProperty("seller_address")
    val sellerAddress: String? = null,

    @JsonProperty("buyer_name")
    val buyerName: String,

    @JsonProperty("buyer_tax_id")
    val buyerTaxId: String? = null,

    @JsonProperty("buyer_address")
    val buyerAddress: String? = null,

    @JsonProperty("client_id")
    val clientId: Long? = null,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("payment_method")
    val paymentMethod: String,

    @JsonProperty("total_net")
    val totalNet: BigDecimal,

    @JsonProperty("total_tax")
    val totalTax: BigDecimal,

    @JsonProperty("total_gross")
    val totalGross: BigDecimal,

    @JsonProperty("currency")
    val currency: String,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("protocol_number")
    val protocolNumber: String? = null,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    @JsonProperty("items")
    val items: List<InvoiceItemDTO>,

    @JsonProperty("attachment")
    val attachment: InvoiceAttachmentDTO? = null
)