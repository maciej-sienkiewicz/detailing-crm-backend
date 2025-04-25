package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime


@Service
class VehicleService(
    private val vehicleRepository: VehicleRepository,
    private val serviceHistoryRepository: ServiceHistoryRepository,
    private val clientRepository: ClientRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val clientVehicleAssociationRepository: ClientVehicleAssociationRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleService::class.java)

    fun createVehicle(vehicle: Vehicle): Vehicle {
        logger.info("Creating new vehicle: ${vehicle.make} ${vehicle.model}, plate: ${vehicle.licensePlate}")
        validateVehicle(vehicle)

        val savedVehicle = vehicleRepository.save(vehicle)
        initializeVehicleStatistics(savedVehicle.id)

        return savedVehicle
    }

    fun updateVehicle(vehicle: Vehicle): Vehicle {
        logger.info("Updating vehicle with ID: ${vehicle.id.value}")

        val existingVehicle = vehicleRepository.findById(vehicle.id)
            ?: throw ResourceNotFoundException("Vehicle", vehicle.id.value)

        validateVehicle(vehicle)

        val updatedVehicle = existingVehicle
            .copy(
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                color = vehicle.color,
                vin = vehicle.vin,
                ownerIds = vehicle.ownerIds
            )

        val savedVehicle = vehicleRepository.save(updatedVehicle)

        logger.info("Updated vehicle with ID: ${savedVehicle.id.value}")
        return savedVehicle
    }

    fun getVehicleById(id: VehicleId): Vehicle? {
        logger.debug("Getting vehicle by ID: ${id.value}")
        return vehicleRepository.findById(id)
    }

    fun getVehicleOwners(id: VehicleId): List<ClientDetails> {
        logger.debug("Getting owners for vehicle ID: ${id.value}")
        val vehicle = vehicleRepository.findById(id)
            ?: throw ResourceNotFoundException("Vehicle", id.value)

        val ownerIds = clientVehicleAssociationRepository.findOwnersByVehicleId(vehicle.id)

        return ownerIds.mapNotNull { clientId ->
            clientRepository.findById(clientId)
        }
    }

    fun getAllVehicles(): List<Vehicle> {
        logger.debug("Getting all vehicles")
        return vehicleRepository.findAll()

    }

    fun getVehiclesByOwnerId(ownerId: String): List<Vehicle> {
        logger.debug("Getting vehicles for owner ID: $ownerId")
        try {
            return vehicleRepository.findByClientId(ClientId(ownerId.toLong()))
        } catch (e: NumberFormatException) {
            logger.warn("Invalid owner ID format: $ownerId")
            return emptyList()
        }
    }

    fun searchVehicles(
        licensePlate: String?,
        make: String?,
        model: String?
    ): List<VehicleWithStats> {
        logger.debug("Searching vehicles with filters: licensePlate=$licensePlate, make=$make, model=$model")

        var result = vehicleRepository.findAll()

        if (!licensePlate.isNullOrBlank()) {
            result = result.filter {
                it.licensePlate!!.contains(licensePlate, ignoreCase = true)
            }
        }

        if (!make.isNullOrBlank()) {
            result = result.filter {
                it.make!!.contains(make, ignoreCase = true)
            }
        }

        if (!model.isNullOrBlank()) {
            result = result.filter {
                it.model!!.contains(model, ignoreCase = true)
            }
        }

        logger.debug("Found ${result.size} vehicles matching filters")
        return result
            .associateBy { vehicleStatisticsRepository.findById(it.id) }
            .map { (stats, vehicle) ->
                VehicleWithStats(
                    vehicle = vehicle,
                    stats = stats
                )
            }
    }

    fun deleteVehicle(id: VehicleId): Boolean {
        logger.info("Deleting vehicle with ID: ${id.value}")

        // Find all owners associated with this vehicle
        val owners = clientVehicleAssociationRepository.findOwnersByVehicleId(id)

        // Update client statistics for all owners
        owners.forEach { clientId ->
            updateClientVehicleCount(clientId, -1)
        }

        return vehicleRepository.deleteById(id)
    }

    fun addServiceHistoryEntry(serviceHistory: ServiceHistory): ServiceHistory {
        logger.info("Adding service history entry for vehicle: ${serviceHistory.vehicleId.value}")
        validateServiceHistory(serviceHistory)

        val savedServiceHistory = serviceHistoryRepository.save(serviceHistory)
        updateVehicleServiceStats(serviceHistory.vehicleId)

        logger.info("Created service history entry with ID: ${savedServiceHistory.id.value}")
        return savedServiceHistory
    }

    fun deleteServiceHistoryEntry(id: ServiceHistoryId): Boolean {
        logger.info("Deleting service history entry with ID: ${id.value}")

        val serviceHistory = serviceHistoryRepository.findById(id)
        if (serviceHistory != null) {
            val deleted = serviceHistoryRepository.deleteById(id)
            if (deleted) {
                updateVehicleServiceStats(serviceHistory.vehicleId)
            }
            return deleted
        }

        return false
    }

    fun getServiceHistoryByVehicleId(vehicleId: VehicleId): List<ServiceHistory> {
        logger.debug("Getting service history for vehicle: ${vehicleId.value}")
        return serviceHistoryRepository.findByVehicleId(vehicleId)
    }

    fun getVehicleStatistics(id: VehicleId): VehicleStats {
        return vehicleStatisticsRepository.findById(id)
    }

    fun addOwnerToVehicle(vehicleId: VehicleId, clientId: ClientId) {
        logger.debug("Adding owner ${clientId.value} to vehicle ${vehicleId.value}")

        // Check if the vehicle exists
        val vehicle = vehicleRepository.findById(vehicleId)
            ?: throw ResourceNotFoundException("Vehicle", vehicleId.value)

        // Check if the client exists
        val client = clientRepository.findById(clientId)
            ?: throw ResourceNotFoundException("Client", clientId.value)

        // Add the association
        clientVehicleAssociationRepository.newAssociation(vehicleId, clientId)

        // Update client statistics
        updateClientVehicleCount(clientId, 1)
    }

    fun removeOwnerFromVehicle(vehicleId: VehicleId, clientId: ClientId): Boolean {
        // This would be a new method to implement in the association repository
        // For now, let's assume it returns true if the association was removed
        val removed = true // clientVehicleAssociationRepository.removeAssociation(vehicleId, clientId)

        if (removed) {
            // Update client statistics
            updateClientVehicleCount(clientId, -1)
        }

        return removed
    }

    private fun updateVehicleServiceStats(vehicleId: VehicleId) {
        logger.debug("Updating service statistics for vehicle: ${vehicleId.value}")

        val vehicle = vehicleRepository.findById(vehicleId) ?: return
        val serviceHistory = serviceHistoryRepository.findByVehicleId(vehicleId)

        if (serviceHistory.isEmpty()) {
            // Reset stats if no history exists
            val updatedVehicle = vehicle.copy(
                totalServices = 0,
                lastServiceDate = null,
                totalSpent = 0.0,
                audit = vehicle.audit.copy(
                    updatedAt = LocalDateTime.now()
                )
            )
            vehicleRepository.save(updatedVehicle)
            return
        }

        // Calculate stats
        val totalServices = serviceHistory.size
        val totalSpent = serviceHistory.sumOf { it.price }
        val lastServiceDate = serviceHistory
            .maxByOrNull { it.date }
            ?.date?.atStartOfDay()

        // Update vehicle
        val updatedVehicle = vehicle.copy(
            totalServices = totalServices,
            lastServiceDate = lastServiceDate,
            totalSpent = totalSpent,
            audit = vehicle.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        vehicleRepository.save(updatedVehicle)
        logger.debug("Updated service statistics for vehicle: ${vehicleId.value}")
    }

    private fun validateVehicle(vehicle: Vehicle) {
        if (vehicle.make.isNullOrBlank()) {
            throw ValidationException("Vehicle make cannot be empty")
        }

        if (vehicle.model.isNullOrBlank()) {
            throw ValidationException("Vehicle model cannot be empty")
        }

        if (vehicle.licensePlate.isNullOrBlank()) {
            throw ValidationException("License plate cannot be empty")
        }

        val currentYear = LocalDate.now().year
        if (vehicle.year != null && (vehicle.year < 1900 || vehicle.year > currentYear + 1)) {
            throw ValidationException("Invalid production year. Year must be between 1900 and ${currentYear + 1}")
        }
    }

    private fun validateServiceHistory(serviceHistory: ServiceHistory) {
        if (serviceHistory.description.isBlank()) {
            throw ValidationException("Service history description cannot be empty")
        }

        if (serviceHistory.serviceType.isBlank()) {
            throw ValidationException("Service type cannot be empty")
        }

        if (serviceHistory.price < 0) {
            throw ValidationException("Service price cannot be negative")
        }

        if (serviceHistory.date.isAfter(LocalDate.now())) {
            throw ValidationException("Service date cannot be in the future")
        }

        // Check if vehicle exists
        vehicleRepository.findById(serviceHistory.vehicleId)
            ?: throw ResourceNotFoundException("Vehicle", serviceHistory.vehicleId.value)
    }

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val stats = VehicleStats(
            vehicleId = vehicleId.value,
            visitNo = 0,
            gmv = "0".toBigDecimal()
        )
        vehicleStatisticsRepository.save(stats)
    }

    private fun updateClientVehicleCount(clientId: ClientId, delta: Long) {
        val clientStats = clientStatisticsRepository.findById(clientId)
            ?: ClientStats(clientId.value, 0, "0".toBigDecimal(), 0)

        val updatedStats = clientStats.copy(
            vehiclesNo = clientStats.vehiclesNo + delta
        )

        clientStatisticsRepository.save(updatedStats)
    }
}

@Service
class VehicleFacade(
    private val vehicleService: VehicleService,
) {
    private val logger = LoggerFactory.getLogger(VehicleFacade::class.java)

    fun createVehicle(vehicle: Vehicle): Vehicle {
        return vehicleService.createVehicle(vehicle)
    }

    fun updateVehicle(vehicle: Vehicle): Vehicle {
        return vehicleService.updateVehicle(vehicle)
    }

    fun getVehicleById(id: VehicleId): Vehicle? {
        return vehicleService.getVehicleById(id)
    }

    fun getVehicleOwners(id: VehicleId): List<ClientDetails> {
        return vehicleService.getVehicleOwners(id)
    }

    fun getAllVehicles(): List<VehicleWithStats> {
        val vehicles = vehicleService.getAllVehicles()

        return vehicles.map { vehicle ->
            val stats = vehicleService.getVehicleStatistics(vehicle.id)
            VehicleWithStats(vehicle, stats)
        }
    }

    fun getVehiclesByOwnerId(ownerId: String): List<VehicleWithStats> {
        val vehicles = vehicleService.getVehiclesByOwnerId(ownerId)

        return vehicles.map { vehicle ->
            val stats = vehicleService.getVehicleStatistics(vehicle.id)
            VehicleWithStats(vehicle, stats)
        }
    }

    fun searchVehicles(
        licensePlate: String?,
        make: String?,
        model: String?
    ): List<VehicleWithStats> {
        return vehicleService.searchVehicles(licensePlate, make, model)
    }

    fun deleteVehicle(id: VehicleId): Boolean {
        return vehicleService.deleteVehicle(id)
    }

    fun addServiceHistoryEntry(serviceHistory: ServiceHistory): ServiceHistory {
        return vehicleService.addServiceHistoryEntry(serviceHistory)
    }

    fun deleteServiceHistoryEntry(id: ServiceHistoryId): Boolean {
        return vehicleService.deleteServiceHistoryEntry(id)
    }

    fun getServiceHistoryByVehicleId(vehicleId: VehicleId): List<ServiceHistory> {
        return vehicleService.getServiceHistoryByVehicleId(vehicleId)
    }

    fun addOwnerToVehicle(vehicleId: VehicleId, clientId: ClientId) {
        vehicleService.addOwnerToVehicle(vehicleId, clientId)
    }

    fun removeOwnerFromVehicle(vehicleId: VehicleId, clientId: ClientId): Boolean {
        return vehicleService.removeOwnerFromVehicle(vehicleId, clientId)
    }
}