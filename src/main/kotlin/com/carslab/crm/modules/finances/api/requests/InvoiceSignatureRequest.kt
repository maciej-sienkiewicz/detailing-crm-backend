package com.carslab.crm.modules.finances.api.requests

import java.util.UUID

data class InvoiceSignatureRequest(
    val tabletId: UUID,
    val customerName: String,
    val signatureTitle: String = "Podpis faktury",
    val instructions: String? = null,
    val timeoutMinutes: Int = 10
)