package com.carslab.crm.clients.domain

import com.carslab.crm.clients.api.CreateVehicleCommand
import com.carslab.crm.clients.api.UpdateVehicleCommand
import com.carslab.crm.clients.domain.model.CreateVehicle
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.model.VehicleStatistics
import com.carslab.crm.clients.domain.model.VehicleWithStatistics
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import com.carslab.crm.clients.domain.port.VehicleRepository
import com.carslab.crm.clients.domain.port.VehicleSearchCriteria
import com.carslab.crm.clients.domain.port.VehicleStatisticsRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class VehicleDomainService(
    private val vehicleRepository: VehicleRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val associationRepository: ClientVehicleAssociationRepository
) {

    fun createVehicle(command: CreateVehicleCommand): Vehicle {
        validateVehicleUniqueness(command.licensePlate, command.vin)

        val vehicle = CreateVehicle(
            make = command.make,
            model = command.model,
            year = command.year,
            licensePlate = command.licensePlate,
            color = command.color,
            vin = command.vin,
            mileage = command.mileage
        )

        val savedVehicle = vehicleRepository.save(vehicle)
        initializeVehicleStatistics(savedVehicle.id)

        return savedVehicle
    }

    fun updateVehicle(id: VehicleId, command: UpdateVehicleCommand): Vehicle {
        val existingVehicle = vehicleRepository.findById(id)
            ?: throw DomainException("Vehicle not found: ${id.value}")

        if (command.licensePlate != existingVehicle.licensePlate || command.vin != existingVehicle.vin) {
            validateVehicleUniqueness(command.licensePlate, command.vin, excludeId = id)
        }

        val updatedVehicle = existingVehicle.copy(
            make = command.make,
            model = command.model,
            year = command.year,
            licensePlate = command.licensePlate,
            color = command.color,
            vin = command.vin,
            mileage = command.mileage,
            audit = existingVehicle.audit.updated()
        )

        return vehicleRepository.save(updatedVehicle)
    }

    @Transactional(readOnly = true)
    fun getVehicleById(id: VehicleId): Vehicle? = vehicleRepository.findById(id)

    @Transactional(readOnly = true)
    fun getVehicleWithStatistics(id: VehicleId): VehicleWithStatistics? {
        val vehicle = vehicleRepository.findById(id) ?: return null
        val statistics = vehicleStatisticsRepository.findByVehicleId(id)
            ?: VehicleStatistics(vehicleId = id.value)

        return VehicleWithStatistics(vehicle, statistics)
    }

    @Transactional(readOnly = true)
    fun searchVehicles(criteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle> {
        return vehicleRepository.searchVehicles(criteria, pageable)
    }

    fun updateStatistics(id: VehicleId, gmv: BigDecimal = BigDecimal.ZERO, counter: Long = 0L) {
        vehicleStatisticsRepository.updateVisitCount(id, counter)
        vehicleStatisticsRepository.updateRevenue(id, gmv)
    }

    fun deleteVehicle(id: VehicleId): Boolean {
        vehicleRepository.findById(id) ?: return false

        // Remove all client associations
        associationRepository.deleteByVehicleId(id)

        // Delete statistics
        vehicleStatisticsRepository.deleteByVehicleId(id)

        return vehicleRepository.deleteById(id)
    }

    private fun validateVehicleUniqueness(licensePlate: String, vin: String?, excludeId: VehicleId? = null) {
        val existingByPlate = vehicleRepository.findByLicensePlate(licensePlate)
        if (existingByPlate != null && existingByPlate.id != excludeId) {
            throw DomainException("Vehicle with license plate $licensePlate already exists")
        }

        if (!vin.isNullOrBlank()) {
            val existingByVin = vehicleRepository.findByVin(vin)
            if (existingByVin != null && existingByVin.id != excludeId) {
                throw DomainException("Vehicle with VIN $vin already exists")
            }
        }
    }

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val statistics = VehicleStatistics(vehicleId = vehicleId.value)
        vehicleStatisticsRepository.save(statistics)
    }
}