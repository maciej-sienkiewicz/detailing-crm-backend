package com.carslab.crm.modules.finances.api.requests

import com.carslab.crm.modules.visits.application.commands.models.valueobjects.OverridenInvoiceServiceItem
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

data class InvoiceSignatureRequest(
    @field:NotNull
    val tabletId: UUID,

    @field:NotBlank
    @field:Size(max = 200)
    val customerName: String,

    @field:Size(max = 200)
    val signatureTitle: String = "Podpis na fakturze",

    @field:Size(max = 1000)
    val instructions: String? = "Proszę podpisać fakturę",

    @field:Positive
    @field:Max(30)
    val timeoutMinutes: Int = 15,
    
    val overridenItems: List<OverridenInvoiceServiceItem>,
    
    val paymentDays: Long = 14,
    
    val paymentMethod: String? = null
)