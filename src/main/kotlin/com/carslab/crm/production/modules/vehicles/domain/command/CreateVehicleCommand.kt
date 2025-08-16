package com.carslab.crm.production.modules.vehicles.domain.command

data class CreateVehicleCommand(
    val companyId: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val ownerIds: List<Long>
)