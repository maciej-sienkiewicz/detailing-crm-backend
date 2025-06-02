package com.carslab.crm.clients.api

data class CreateVehicleCommand(
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
    val ownerIds: List<String> = emptyList()
)

data class UpdateVehicleCommand(
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null
)

data class CreateClientCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)

data class UpdateClientCommand(
    val id: String? = null,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    val taxId: String? = null,
    val notes: String? = null
)