package com.carslab.crm.modules.finances.api.requests

import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.OverridenInvoiceServiceItem
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

data class InvoiceGenerationFromVisitRequest(
    @field:NotBlank
    @JsonProperty("visit_id")
    val visitId: String,

    @JsonProperty("overriden_items")
    val overridenItems: List<CreateServiceCommand> = emptyList(),

    @field:Positive
    @field:Max(365)
    @JsonProperty("payment_days")
    val paymentDays: Long = 14,

    @JsonProperty("payment_method")
    val paymentMethod: String? = null,

    @JsonProperty("invoice_title")
    val invoiceTitle: String? = null,

    @JsonProperty("notes")
    val notes: String? = null
)