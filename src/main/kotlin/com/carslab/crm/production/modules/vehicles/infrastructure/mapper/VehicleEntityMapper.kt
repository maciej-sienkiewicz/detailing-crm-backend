package com.carslab.crm.production.modules.vehicles.infrastructure.mapper

import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import com.carslab.crm.production.modules.vehicles.infrastructure.entity.VehicleEntity
import com.carslab.crm.production.modules.vehicles.infrastructure.entity.VehicleStatisticsEntity

fun Vehicle.toEntity(): VehicleEntity {
    return VehicleEntity(
        id = if (this.id.value == 0L) null else this.id.value,
        companyId = this.companyId,
        make = this.make,
        model = this.model,
        year = this.year,
        licensePlate = this.licensePlate,
        color = this.color,
        vin = this.vin,
        mileage = this.mileage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}

fun VehicleEntity.toDomain(): Vehicle {
    return Vehicle(
        id = VehicleId.of(this.id!!),
        companyId = this.companyId,
        make = this.make,
        model = this.model,
        year = this.year,
        licensePlate = this.licensePlate,
        color = this.color,
        vin = this.vin,
        mileage = this.mileage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}

fun VehicleStatistics.toEntity(): VehicleStatisticsEntity {
    return VehicleStatisticsEntity(
        vehicleId = this.vehicleId.value,
        visitCount = this.visitCount,
        totalRevenue = this.totalRevenue,
        lastVisitDate = this.lastVisitDate,
        updatedAt = this.updatedAt
    )
}

fun VehicleStatisticsEntity.toDomain(): VehicleStatistics {
    return VehicleStatistics(
        vehicleId = VehicleId.of(this.vehicleId),
        visitCount = this.visitCount,
        totalRevenue = this.totalRevenue,
        lastVisitDate = this.lastVisitDate,
        updatedAt = this.updatedAt
    )
}