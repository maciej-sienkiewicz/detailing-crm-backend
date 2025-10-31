package com.carslab.crm.production.modules.vehicles.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class EnhancedVehicle(
    val vehicle: Vehicle,
    val statistics: VehicleStatistics?,
    val owners: List<VehicleOwner> = emptyList()
) {
    val id: VehicleId get() = vehicle.id
    val companyId: Long get() = vehicle.companyId
    val make: String get() = vehicle.make
    val model: String get() = vehicle.model
    val year: Int? get() = vehicle.year
    val licensePlate: String get() = vehicle.licensePlate
    val color: String? get() = vehicle.color
    val vin: String? get() = vehicle.vin
    val mileage: Long? get() = vehicle.mileage
    val displayName: String get() = vehicle.displayName
    val createdAt: LocalDateTime get() = vehicle.createdAt
    val updatedAt: LocalDateTime get() = vehicle.updatedAt

    val visitCount: Long get() = statistics?.visitCount ?: 0L
    val lastVisitDate: LocalDateTime? get() = statistics?.lastVisitDate
}

data class VehicleOwner(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String?,
    val phone: String?
)