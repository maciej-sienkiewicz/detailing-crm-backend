package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.command.CreateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.command.UpdateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VehicleDomainService(
    private val vehicleRepository: VehicleRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleDomainService::class.java)

    fun createVehicle(command: CreateVehicleCommand): Vehicle {
        logger.debug("Creating vehicle: {} {} for company: {}", command.make, command.model, command.companyId)

        validateVehicleUniqueness(command.licensePlate, command.vin, command.companyId)

        val vehicle = Vehicle(
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
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedVehicle = vehicleRepository.save(vehicle)
        initializeVehicleStatistics(savedVehicle.id)

        logger.info("Vehicle created: {} for company: {}", savedVehicle.id.value, command.companyId)
        return savedVehicle
    }

    fun updateVehicle(vehicleId: VehicleId, command: UpdateVehicleCommand, companyId: Long): Vehicle {
        logger.debug("Updating vehicle: {} for company: {}", vehicleId.value, companyId)

        val existingVehicle = getVehicleForCompany(vehicleId, companyId)

        if (command.licensePlate != existingVehicle.licensePlate || command.vin != existingVehicle.vin) {
            validateVehicleUniqueness(command.licensePlate, command.vin, companyId, excludeVehicleId = vehicleId)
        }

        val updatedVehicle = existingVehicle.update(
            make = command.make.trim(),
            model = command.model.trim(),
            year = command.year,
            licensePlate = command.licensePlate.trim(),
            color = command.color?.trim(),
            vin = command.vin?.trim(),
            mileage = command.mileage
        )

        val saved = vehicleRepository.save(updatedVehicle)
        logger.info("Vehicle updated: {} for company: {}", vehicleId.value, companyId)
        return saved
    }

    fun getVehicleForCompany(vehicleId: VehicleId, companyId: Long): Vehicle {
        val vehicle = vehicleRepository.findById(vehicleId)
            ?: throw BusinessException("Vehicle not found: ${vehicleId.value}")

        if (!vehicle.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to vehicle: ${vehicleId.value}")
        }

        return vehicle
    }

    fun deleteVehicle(vehicleId: VehicleId, companyId: Long): Boolean {
        logger.debug("Deleting vehicle: {} for company: {}", vehicleId.value, companyId)

        getVehicleForCompany(vehicleId, companyId)

        vehicleStatisticsRepository.deleteByVehicleId(vehicleId)
        val deleted = vehicleRepository.deleteById(vehicleId)

        if (deleted) {
            logger.info("Vehicle deleted: {} for company: {}", vehicleId.value, companyId)
        }

        return deleted
    }

    private fun validateVehicleUniqueness(
        licensePlate: String,
        vin: String?,
        companyId: Long,
        excludeVehicleId: VehicleId? = null
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

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val statistics = VehicleStatistics(vehicleId = vehicleId)
        vehicleStatisticsRepository.save(statistics)
    }
}