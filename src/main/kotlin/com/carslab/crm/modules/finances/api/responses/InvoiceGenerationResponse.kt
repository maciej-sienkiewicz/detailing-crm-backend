package com.carslab.crm.modules.finances.api.responses

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InvoiceGenerationResponse(
    val success: Boolean,
    @JsonProperty("invoice_id")
    val invoiceId: String,
    @JsonProperty("invoice_number")
    val invoiceNumber: String,
    @JsonProperty("total_amount")
    val totalAmount: BigDecimal,
    @JsonProperty("issued_date")
    val issuedDate: LocalDate,
    @JsonProperty("due_date")
    val dueDate: LocalDate,
    @JsonProperty("payment_method")
    val paymentMethod: String,
    @JsonProperty("customer_name")
    val customerName: String,
    @JsonProperty("document_status")
    val documentStatus: String,
    @JsonProperty("download_url")
    val downloadUrl: String? = null,
    val message: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("visit_id")
    val visitId: String
)