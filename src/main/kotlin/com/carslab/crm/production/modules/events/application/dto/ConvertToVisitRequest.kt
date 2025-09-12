package com.carslab.crm.production.modules.events.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

data class ConvertToVisitRequest(
    @field:NotNull(message = "Client ID is required")
    @JsonProperty("client_id")
    val clientId: Long,

    @field:NotNull(message = "Vehicle ID is required")
    @JsonProperty("vehicle_id")
    val vehicleId: Long,

    @JsonProperty("additional_services")
    val additionalServices: List<AdditionalServiceRequest> = emptyList(),

    val notes: String? = null
)

data class AdditionalServiceRequest(
    val name: String,
    @JsonProperty("base_price")
    val basePrice: java.math.BigDecimal,
    val quantity: Long = 1
)