package com.carslab.crm.production.modules.vehicles.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateVehicleRequest(
    @field:NotBlank(message = "Make is required")
    @field:Size(max = 100, message = "Make cannot exceed 100 characters")
    val make: String,

    @field:NotBlank(message = "Model is required")
    @field:Size(max = 100, message = "Model cannot exceed 100 characters")
    val model: String,

    val year: Int? = null,

    @field:NotBlank(message = "License plate is required")
    @field:Size(max = 20, message = "License plate cannot exceed 20 characters")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @field:Size(max = 50, message = "Color cannot exceed 50 characters")
    val color: String? = null,

    @field:Size(max = 17, message = "VIN cannot exceed 17 characters")
    val vin: String? = null,

    val mileage: Long? = null,

    @JsonProperty("owner_ids")
    val ownerIds: List<Long> = emptyList()
)

data class UpdateVehicleRequest(
    @field:NotBlank(message = "Make is required")
    @field:Size(max = 100, message = "Make cannot exceed 100 characters")
    val make: String,

    @field:NotBlank(message = "Model is required")
    @field:Size(max = 100, message = "Model cannot exceed 100 characters")
    val model: String,

    val year: Int? = null,

    @field:NotBlank(message = "License plate is required")
    @field:Size(max = 20, message = "License plate cannot exceed 20 characters")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @field:Size(max = 50, message = "Color cannot exceed 50 characters")
    val color: String? = null,

    @field:Size(max = 17, message = "VIN cannot exceed 17 characters")
    val vin: String? = null,

    val mileage: Long? = null,

    @JsonProperty("owner_ids")
    val ownerIds: List<Long> = emptyList()
)