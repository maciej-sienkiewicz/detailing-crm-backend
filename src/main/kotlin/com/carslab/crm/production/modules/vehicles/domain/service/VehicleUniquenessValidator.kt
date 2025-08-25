package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class VehicleUniquenessValidator(
    private val vehicleRepository: VehicleRepository
) {
    fun validateForCreation(licensePlate: String, vin: String?, companyId: Long) {
        val existingByPlate = vehicleRepository.findByLicensePlate(licensePlate, companyId)
        if (existingByPlate != null) {
            throw BusinessException("Vehicle with license plate $licensePlate already exists")
        }

        if (!vin.isNullOrBlank()) {
            val existingByVin = vehicleRepository.findByVin(vin, companyId)
            if (existingByVin != null) {
                throw BusinessException("Vehicle with VIN $vin already exists")
            }
        }
    }

    fun validateForUpdate(
        licensePlate: String,
        vin: String?,
        companyId: Long,
        excludeVehicleId: VehicleId
    ) {
        val existingByPlate = vehicleRepository.findByLicensePlate(licensePlate, companyId)
        if (existingByPlate != null && existingByPlate.id != excludeVehicleId) {
            throw BusinessException("Vehicle with license plate $licensePlate already exists")
        }

        if (!vin.isNullOrBlank()) {
            val existingByVin = vehicleRepository.findByVin(vin, companyId)
            if (existingByVin != null && existingByVin.id != excludeVehicleId) {
                throw BusinessException("Vehicle with VIN $vin already exists")
            }
        }
    }
}