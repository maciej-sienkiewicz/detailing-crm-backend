package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

data class ServiceHistoryResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("vehicle_id")
    val vehicleId: String,

    @JsonProperty("date")
    val date: LocalDate,

    @JsonProperty("service_type")
    val serviceType: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("price")
    val price: Double,

    @JsonProperty("protocol_id")
    val protocolId: String? = null,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)