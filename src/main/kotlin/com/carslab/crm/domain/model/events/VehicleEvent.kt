package com.carslab.crm.domain.model.events

import com.carslab.crm.modules.clients.domain.model.VehicleId
import java.time.LocalDateTime

sealed class VehicleEvent {
    abstract val vehicleId: VehicleId
    abstract val timestamp: LocalDateTime

    data class VehicleCreated(
        override val vehicleId: VehicleId,
        val make: String,
        val model: String,
        val year: Int?,
        val licensePlate: String,
        val color: String?,
        val vin: String?,
        val mileage: Long?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : VehicleEvent()

    data class VehicleUpdated(
        override val vehicleId: VehicleId,
        val make: String,
        val model: String,
        val year: Int?,
        val licensePlate: String,
        val color: String?,
        val vin: String?,
        val mileage: Long?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : VehicleEvent()

    data class ServiceRecorded(
        override val vehicleId: VehicleId,
        val serviceDate: LocalDateTime,
        val amount: java.math.BigDecimal,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : VehicleEvent()

    data class VehicleDeleted(
        override val vehicleId: VehicleId,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : VehicleEvent()
}