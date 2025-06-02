package com.carslab.crm.clients.api.requests

import java.math.BigDecimal
import java.time.LocalDate

data class ClientRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)

data class VehicleRequest(
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
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