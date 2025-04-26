package com.carslab.crm.api.model.commands

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class ReleaseVehicleCommand(

    @JsonProperty("payment_method")
    val paymentMethod: String,

    @JsonProperty("document_type")
    val documentType: String,

    @JsonProperty("additional_notes")
    val additionalNotes: String? = null,
)