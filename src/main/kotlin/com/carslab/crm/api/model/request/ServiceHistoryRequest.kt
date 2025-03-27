package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ServiceHistoryRequest(
    @JsonProperty("id")
    var id: String? = null,

    @JsonProperty("vehicle_id")
    var vehicleId: String? = null,

    @JsonProperty("date")
    val date: LocalDate,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("protocol_id")
    val protocolId: String? = null
)