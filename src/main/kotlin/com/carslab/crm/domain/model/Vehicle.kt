package com.carslab.crm.domain.model

import com.carslab.crm.domain.model.stats.VehicleStats
import java.time.LocalDateTime
import java.util.UUID

data class VehicleId(val value: Long) {
    companion object {
        fun generate(): VehicleId = VehicleId(System.currentTimeMillis())
    }
}

data class Vehicle(
    val id: VehicleId,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val totalServices: Int = 0,
    val lastServiceDate: LocalDateTime? = null,
    val totalSpent: Double = 0.0,
    val ownerIds: List<String> = emptyList(),
    val audit: Audit
)

data class VehicleWithStats(
    val vehicle: Vehicle,
    val stats: VehicleStats
)