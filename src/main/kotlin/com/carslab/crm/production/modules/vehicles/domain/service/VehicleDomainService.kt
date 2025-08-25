package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.command.CreateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.command.UpdateVehicleCommand
import com.carslab.crm.production.modules.vehicles.domain.model.*
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleRepository
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleSearchCriteria
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class VehicleDomainService(
    private val vehicleRepository: VehicleRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val vehicleUniquenessValidator: VehicleUniquenessValidator,
    private val vehicleStatisticsInitializer: VehicleStatisticsInitializer,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val enhancedVehicleBuilder: EnhancedVehicleBuilder
) {
    private val logger = LoggerFactory.getLogger(VehicleDomainService::class.java)

    fun createVehicle(command: CreateVehicleCommand): Vehicle {
        logger.debug("Creating vehicle: {} {} for company: {}", command.make, command.model, command.companyId)

        vehicleUniquenessValidator.validateForCreation(command.licensePlate, command.vin, command.companyId)

        val vehicle = Vehicle.from(command)
        val savedVehicle = vehicleRepository.save(vehicle)

        vehicleStatisticsInitializer.initialize(savedVehicle.id)

        logger.info("Vehicle created: {} for company: {}", savedVehicle.id.value, command.companyId)
        return savedVehicle
    }

    fun updateVehicle(vehicleId: VehicleId, command: UpdateVehicleCommand, companyId: Long): Vehicle {
        logger.debug("Updating vehicle: {} for company: {}", vehicleId.value, companyId)

        val existingVehicle = vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        if (command.licensePlate != existingVehicle.licensePlate || command.vin != existingVehicle.vin) {
            vehicleUniquenessValidator.validateForUpdate(command.licensePlate, command.vin, companyId, vehicleId)
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
        return vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)
    }

    fun getEnhancedVehicle(vehicleId: VehicleId, companyId: Long): EnhancedVehicle {
        val vehicle = getVehicleForCompany(vehicleId, companyId)
        return enhancedVehicleBuilder.buildSingle(vehicle, companyId)
    }

    fun getVehicles(companyId: Long, searchCriteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        logger.debug("Fetching vehicles for company: {} with search criteria", companyId)
        return vehicleRepository.searchVehicles(companyId, searchCriteria, pageable)
    }

    fun enhanceVehicles(vehicles: Page<Vehicle>, companyId: Long): Page<EnhancedVehicle> {
        return enhancedVehicleBuilder.buildMultiple(vehicles, companyId)
    }

    fun deleteVehicle(vehicleId: VehicleId, companyId: Long): Boolean {
        logger.debug("Deleting vehicle: {} for company: {}", vehicleId.value, companyId)

        vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        vehicleStatisticsRepository.deleteByVehicleId(vehicleId)
        val deleted = vehicleRepository.deleteById(vehicleId)

        if (deleted) {
            logger.info("Vehicle deleted: {} for company: {}", vehicleId.value, companyId)
        }

        return deleted
    }

    fun exists(vehicleId: VehicleId, companyId: Long): Boolean {
        return vehicleRepository.existsByIdAndCompanyId(vehicleId, companyId)
    }

    fun getVehiclesByIds(vehiclesIds: List<VehicleId>): List<Vehicle> {
        return vehicleRepository.findAllById(vehiclesIds)
    }
}