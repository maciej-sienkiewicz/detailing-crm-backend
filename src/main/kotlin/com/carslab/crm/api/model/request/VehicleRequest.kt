package com.carslab.crm.api.model.request

import com.carslab.crm.infrastructure.validation.ValidLicensePlate
import com.carslab.crm.infrastructure.validation.ValidVin
import com.carslab.crm.infrastructure.validation.ValidYear
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "Request object for creating or updating a vehicle")
data class VehicleRequest(
    @JsonProperty("id")
    @Schema(description = "Vehicle ID - required for updates, ignored for creation")
    var id: String? = null,

    @JsonProperty("make")
    @field:NotBlank(message = "{validation.vehicleRequest.make.notBlank}")
    @field:Size(min = 1, max = 50)
    @Schema(description = "Vehicle manufacturer", example = "Toyota", required = true)
    val make: String,

    @JsonProperty("model")
    @field:NotBlank(message = "{validation.vehicleRequest.model.notBlank}")
    @field:Size(min = 1, max = 50)
    @Schema(description = "Vehicle model", example = "Camry", required = true)
    val model: String,

    @JsonProperty("year")
    @field:ValidYear(message = "{validation.vehicleRequest.year.range}")
    @Schema(description = "Vehicle production year", example = "2023", required = true)
    val year: Int,

    @JsonProperty("license_plate")
    @field:NotBlank(message = "{validation.vehicleRequest.licensePlate.notBlank}")
    @field:ValidLicensePlate
    @field:Size(min = 2, max = 15)
    @Schema(description = "Vehicle license plate number", example = "ABC-1234", required = true)
    val licensePlate: String,

    @JsonProperty("color")
    @field:Size(max = 30)
    @Schema(description = "Vehicle color", example = "Silver")
    val color: String? = null,

    @JsonProperty("vin")
    @field:ValidVin
    @field:Size(max = 17)
    @Schema(description = "Vehicle Identification Number", example = "1HGCM82633A123456")
    val vin: String? = null,

    @JsonProperty("owner_ids")
    @field:NotEmpty(message = "{validation.vehicleRequest.ownerIds.notEmpty}")
    @Schema(description = "List of owner IDs associated with this vehicle", required = true)
    val ownerIds: List<String>
)