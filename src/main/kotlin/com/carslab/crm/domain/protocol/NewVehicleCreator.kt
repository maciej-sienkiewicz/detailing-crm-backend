package com.carslab.crm.domain.protocol

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NewVehicleCreator(
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(NewVehicleCreator::class.java)

    fun getVehicle(protocol: CarReceptionProtocol): Vehicle {
        val vehicle = findOrCreateVehicle(protocol)

        // Ensure the client is listed as an owner of the vehicle
        if (protocol.client.id != null && !vehicle.ownerIds.contains(protocol.client.id.toString())) {
            val updatedVehicle = addOwnerToVehicle(vehicle, protocol.client.id.toString())
            updateClientStatistics(protocol.client.id)
            return updatedVehicle
        }

        return vehicle
    }

    private fun findOrCreateVehicle(protocol: CarReceptionProtocol): Vehicle {
        // Try to find by VIN or license plate
        val existingVehicle = vehicleRepository.findByVinOrLicensePlate(
            protocol.vehicle.vin,
            protocol.vehicle.licensePlate
        )

        if (existingVehicle != null) {
            logger.debug("Found existing vehicle with license plate: ${protocol.vehicle.licensePlate}")
            return existingVehicle
        }

        // Create new vehicle
        logger.info("Creating new vehicle: ${protocol.vehicle.make} ${protocol.vehicle.model} (${protocol.vehicle.licensePlate})")

        val newVehicle = Vehicle(
            id = VehicleId.generate(),
            make = protocol.vehicle.make,
            model = protocol.vehicle.model,
            year = protocol.vehicle.productionYear,
            licensePlate = protocol.vehicle.licensePlate,
            color = protocol.vehicle.color,
            vin = protocol.vehicle.vin,
            totalServices = 0,
            lastServiceDate = null,
            totalSpent = 0.0,
            ownerIds = protocol.client.id?.let { listOf(it.toString()) } ?: emptyList(),
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val savedVehicle = vehicleRepository.save(newVehicle)

        // Initialize vehicle statistics
        initializeVehicleStatistics(savedVehicle.id)

        // Update client statistics if client ID exists
        protocol.client.id?.let { updateClientStatistics(it) }

        return savedVehicle
    }

    private fun addOwnerToVehicle(vehicle: Vehicle, ownerId: String): Vehicle {
        val updatedVehicle = vehicle.copy(
            ownerIds = vehicle.ownerIds + ownerId,
            audit = vehicle.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        return vehicleRepository.save(updatedVehicle)
    }

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val stats = VehicleStats(
            vehicleId = vehicleId.value,
            visitNo = 0,
            gmv = "0".toBigDecimal()
        )

        vehicleStatisticsRepository.save(stats)
    }

    private fun updateClientStatistics(clientId: Long) {
        val clientStats = clientStatisticsRepository.findById(ClientId(clientId))
            ?: ClientStats(clientId, 0, "0".toBigDecimal(), 0)

        val updatedStats = clientStats.copy(
            vehiclesNo = clientStats.vehiclesNo + 1
        )

        clientStatisticsRepository.save(updatedStats)
    }
}