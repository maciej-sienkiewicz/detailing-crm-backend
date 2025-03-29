package com.carslab.crm.presentation.mapper

import com.carslab.crm.api.model.request.VehicleRequest
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.api.model.response.VehicleResponse
import com.carslab.crm.domain.model.VehicleWithStats
import java.time.LocalDateTime

object VehicleMapper {

    fun toDomain(request: VehicleRequest): Vehicle {
        val now = LocalDateTime.now()

        return Vehicle(
            id = request.id?.let { VehicleId(it.toLong()) } ?: VehicleId.generate(),
            make = request.make,
            model = request.model,
            year = request.year,
            licensePlate = request.licensePlate,
            color = request.color,
            vin = request.vin,
            totalServices = 0, // Domyślnie dla nowego pojazdu
            lastServiceDate = null, // Domyślnie dla nowego pojazdu
            totalSpent = 0.0, // Domyślnie dla nowego pojazdu
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun toResponse(vehicle: Vehicle): VehicleResponse {
        return VehicleResponse(
            id = vehicle.id.value.toString(),
            make = vehicle.make ?: "",
            model = vehicle.model ?: "",
            year = vehicle.year ?: 0,
            licensePlate = vehicle.licensePlate ?: "",
            color = vehicle.color,
            vin = vehicle.vin,
            totalServices = vehicle.totalServices,
            lastServiceDate = vehicle.lastServiceDate,
            totalSpent = vehicle.totalSpent,
            createdAt = vehicle.audit.createdAt,
            updatedAt = vehicle.audit.updatedAt
        )
    }

    fun toResponse(vehicle: VehicleWithStats): VehicleResponse {
        return VehicleResponse(
            id = vehicle.vehicle.id.value.toString(),
            make = vehicle.vehicle.make ?: "",
            model = vehicle.vehicle.model ?: "",
            year = vehicle.vehicle.year ?: 0,
            licensePlate = vehicle.vehicle.licensePlate ?: "",
            color = vehicle.vehicle.color,
            vin = vehicle.vehicle.vin,
            totalServices = vehicle.stats.visitNo.toInt(),
            lastServiceDate = vehicle.vehicle.lastServiceDate,
            totalSpent = vehicle.stats.gmv.toDouble(),
            createdAt = vehicle.vehicle.audit.createdAt,
            updatedAt = vehicle.vehicle.audit.updatedAt
        )
    }
}