package com.carslab.crm.modules.clients.api.requests

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class ClientRequest(
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("last_name")
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    @JsonProperty("tax_id")
    val taxId: String? = null,
    val notes: String? = null
)

data class VehicleRequest(
    val make: String,
    val model: String,
    val year: Int?,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
    @JsonProperty("owner_ids")
    val ownerIds: List<Long> = emptyList()
)

data class ContactAttemptRequest(
    var id: String? = null,
    val clientId: String,
    val type: String,
    val description: String,
    val result: String,
    val date: String? = null,
    val notes: String? = null
)

data class ServiceHistoryRequest(
    val vehicleId: String? = null,
    val serviceType: String,
    val description: String,
    val price: BigDecimal,
    val date: LocalDate
)