package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.command.CreateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class VehicleFactory {
    fun create(command: CreateVehicleCommand): Vehicle {
        return Vehicle(
            id = VehicleId(0),
            companyId = command.companyId,
            make = command.make.trim(),
            model = command.model.trim(),
            year = command.year,
            licensePlate = command.licensePlate.trim(),
            color = command.color?.trim(),
            vin = command.vin?.trim(),
            mileage = command.mileage,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}