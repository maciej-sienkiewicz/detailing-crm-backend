package com.carslab.crm.modules.visits.api.commands

import com.fasterxml.jackson.annotation.JsonProperty

data class ReleaseVehicleRequest(

    @JsonProperty("payment_method")
    val paymentMethod: String,

    @JsonProperty("document_type")
    val documentType: String,

    @JsonProperty("additional_notes")
    val additionalNotes: String? = null,

    @JsonProperty("overriden_items")
    val overridenItems: List<CreateServiceCommand>? = null,
    
    @JsonProperty("payment_days")
    val paymentDays: Long = 14,
)