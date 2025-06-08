package com.carslab.crm.modules.visits.api.commands

import com.fasterxml.jackson.annotation.JsonProperty

data class ReleaseVehicleCommand(

    @JsonProperty("payment_method")
    val paymentMethod: String,

    @JsonProperty("document_type")
    val documentType: String,

    @JsonProperty("additional_notes")
    val additionalNotes: String? = null,
)