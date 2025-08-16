package com.carslab.crm.production.modules.vehicles.domain.model

import java.time.LocalDateTime

@JvmInline
value class VehicleId(val value: Long) {
    companion object {
        fun of(value: Long): VehicleId = VehicleId(value)
    }
}

data class Vehicle(
    val id: VehicleId,
    val companyId: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long
) {
    val displayName: String get() = "$make $model ($licensePlate)"

    fun canBeAccessedBy(companyId: Long): Boolean {
        return this.companyId == companyId
    }

    fun update(
        make: String,
        model: String,
        year: Int?,
        licensePlate: String,
        color: String?,
        vin: String?,
        mileage: Long?
    ): Vehicle {
        return copy(
            make = make,
            model = model,
            year = year,
            licensePlate = licensePlate,
            color = color,
            vin = vin,
            mileage = mileage,
            updatedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
}