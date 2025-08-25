package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class VehicleAccessValidator(
    private val vehicleRepository: VehicleRepository
) {
    fun getVehicleForCompany(vehicleId: VehicleId, companyId: Long): Vehicle {
        val vehicle = vehicleRepository.findById(vehicleId)
            ?: throw BusinessException("Vehicle not found: ${vehicleId.value}")

        if (!vehicle.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to vehicle: ${vehicleId.value}")
        }

        return vehicle
    }
}