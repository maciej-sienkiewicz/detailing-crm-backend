package com.carslab.crm.modules.finances.api.requests

import com.carslab.crm.modules.visits.application.commands.models.valueobjects.OverridenInvoiceServiceItem
import java.util.UUID

data class InvoiceSignatureRequest(
    @field:jakarta.validation.constraints.NotNull
    val tabletId: UUID,

    @field:jakarta.validation.constraints.NotBlank
    @field:jakarta.validation.constraints.Size(max = 200)
    val customerName: String,

    @field:jakarta.validation.constraints.Size(max = 200)
    val signatureTitle: String = "Podpis na fakturze",

    @field:jakarta.validation.constraints.Size(max = 1000)
    val instructions: String? = "Proszę podpisać fakturę",

    @field:jakarta.validation.constraints.Positive
    @field:jakarta.validation.constraints.Max(30)
    val timeoutMinutes: Int = 15,

    val overridenItems: List<OverridenInvoiceServiceItem>,

    val paymentDays: Long = 14
)