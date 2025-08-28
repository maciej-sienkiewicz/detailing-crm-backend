package com.carslab.crm.production.modules.vehicles.domain.command

import com.carslab.crm.production.modules.clients.domain.model.ClientId

data class UpdateVehicleCommand(
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val ownerIds: List<ClientId>
)