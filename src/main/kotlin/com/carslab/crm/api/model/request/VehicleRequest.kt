package com.carslab.crm.api.model.request

import com.fasterxml.jackson.annotation.JsonProperty

data class VehicleRequest(
    @JsonProperty("id")
    var id: String? = null,

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

    @JsonProperty("owner_ids")
    val ownerIds: List<String>
)