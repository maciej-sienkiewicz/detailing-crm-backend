package com.carslab.crm.api.model.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDateTime

data class VehicleResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("make")
    val make: String,

    @JsonProperty("model")
    val model: String,

    @JsonProperty("year")
    val year: Int,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("color")
    val color: String? = null,

    @JsonProperty("vin")
    val vin: String? = null,

    @JsonProperty("total_services")
    val totalServices: Int,

    @JsonProperty("last_service_date")
    val lastServiceDate: LocalDateTime? = null,

    @JsonProperty("total_spent")
    val totalSpent: Double,

    @JsonProperty("owner_ids")
    val ownerIds: List<String>,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)