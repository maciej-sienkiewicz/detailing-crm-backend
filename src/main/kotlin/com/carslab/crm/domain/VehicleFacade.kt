package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.*

@Service
class VehicleFacade(
    private val vehicleRepository: VehicleRepository,
    private val serviceHistoryRepository: ServiceHistoryRepository,
    private val clientsRepository: ClientRepository,
    private val clientStatsRepository: ClientStatisticsRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
) {

    private val logger = LoggerFactory.getLogger(VehicleFacade::class.java)

    fun createVehicle(vehicle: Vehicle): Vehicle {
        logger.info("Creating new vehicle: ${vehicle.make} ${vehicle.model}, plate: ${vehicle.licensePlate}")

        validateVehicle(vehicle)

        // Zapisujemy pojazd w repozytorium
        val savedVehicle = vehicleRepository.save(vehicle)
        logger.info("Created vehicle with ID: ${savedVehicle.id.value}")
        return savedVehicle
    }

    fun updateVehicle(vehicle: Vehicle): Vehicle {
        logger.info("Updating vehicle with ID: ${vehicle.id.value}")

        // Sprawdzamy czy pojazd istnieje
        val existingVehicle = vehicleRepository.findById(vehicle.id)
            ?: throw IllegalArgumentException("Vehicle with ID ${vehicle.id.value} not found")

        // Walidacja
        validateVehicle(vehicle)

        // Zachowujemy statystyki serwisowe z istniejącego pojazdu
        val updatedVehicle = vehicle.copy(
            totalServices = existingVehicle.totalServices,
            lastServiceDate = existingVehicle.lastServiceDate,
            totalSpent = existingVehicle.totalSpent,
            audit = vehicle.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        // Zapisujemy zaktualizowany pojazd
        val savedVehicle = vehicleRepository.save(updatedVehicle)

        val previousOwners = existingVehicle.ownerIds.toSet()
        val currentOwners = savedVehicle.ownerIds.toSet()

        previousOwners.minus(currentOwners)
            .map { clientStatsRepository.findById(ClientId(it.toLong())) ?: ClientStats(it.toLong(), 0, "0".toBigDecimal(), 0) }
            .map { it.copy(vehiclesNo = it.vehiclesNo - 1) }
            .forEach { clientStatsRepository.save(it) }

        currentOwners.minus(previousOwners)
            .map { clientStatsRepository.findById(ClientId(it.toLong())) ?: ClientStats(it.toLong(), 0, "0".toBigDecimal(), 0) }
            .map { it.copy(vehiclesNo = it.vehiclesNo + 1) }
            .forEach { clientStatsRepository.save(it) }

        logger.info("Updated vehicle with ID: ${savedVehicle.id.value}")
        return savedVehicle
    }

    fun getVehicleById(id: VehicleId): Vehicle? {
        logger.debug("Getting vehicle by ID: ${id.value}")
        return vehicleRepository.findById(id)
    }

    fun getVehicleOwners(id: VehicleId): List<ClientDetails> {
        logger.debug("Getting vehicle by ID: ${id.value}")
        val vehicle = vehicleRepository.findById(id)
        return vehicle?.ownerIds?.map { clientsRepository.findById(ClientId(it.toLong())) }?.filterNotNull() ?: emptyList()
    }

    fun getAllVehicles(): List<VehicleWithStats> {
        logger.debug("Getting all vehicles")
        val vehicles = vehicleRepository.findAll()
        val stats: Map<Long, VehicleStats> = vehicles.map {
            vehicleStatisticsRepository.findById(it.id)
        }.associateBy { it.vehicleId }

        return vehicles.map { VehicleWithStats(it, stats[it.id.value]!!) }
    }

    fun getVehiclesByOwnerId(ownerId: String): List<VehicleWithStats> {
        logger.debug("Getting vehicles for owner ID: $ownerId")
        val vehicles = vehicleRepository.findByOwnerId(ownerId)
        val stats: Map<Long, VehicleStats> = vehicles.map {
            vehicleStatisticsRepository.findById(it.id)
        }.associateBy { it.vehicleId }

        return vehicles.map { VehicleWithStats(it, stats[it.id.value]!!) }
    }

    fun searchVehicles(
        licensePlate: String?,
        make: String?,
        model: String?
    ): List<Vehicle> {
        logger.debug("Searching vehicles with filters: licensePlate=$licensePlate, make=$make, model=$model")

        // Pobieramy wszystkie pojazdy, a następnie filtrujemy je według podanych kryteriów
        var result = vehicleRepository.findAll()

        // Filtrowanie według numeru rejestracyjnego
        if (!licensePlate.isNullOrBlank()) {
            result = result.filter {
                it.licensePlate.contains(licensePlate, ignoreCase = true)
            }
        }

        // Filtrowanie według marki
        if (!make.isNullOrBlank()) {
            result = result.filter {
                it.make.contains(make, ignoreCase = true)
            }
        }

        // Filtrowanie według modelu
        if (!model.isNullOrBlank()) {
            result = result.filter {
                it.model.contains(model, ignoreCase = true)
            }
        }

        logger.debug("Found ${result.size} vehicles matching filters")
        return result
    }

    fun deleteVehicle(id: VehicleId): Boolean {
        logger.info("Deleting vehicle with ID: ${id.value}")
        return vehicleRepository.deleteById(id)
    }

    fun addServiceHistoryEntry(serviceHistory: ServiceHistory): ServiceHistory {
        logger.info("Adding service history entry for vehicle: ${serviceHistory.vehicleId.value}")

        validateServiceHistory(serviceHistory)

        // Zapisujemy wpis historii serwisowej
        val savedServiceHistory = serviceHistoryRepository.save(serviceHistory)

        // Aktualizujemy statystyki pojazdu
        updateVehicleServiceStats(serviceHistory.vehicleId)

        logger.info("Created service history entry with ID: ${savedServiceHistory.id.value}")
        return savedServiceHistory
    }

    fun deleteServiceHistoryEntry(id: ServiceHistoryId): Boolean {
        logger.info("Deleting service history entry with ID: ${id.value}")

        // Najpierw pobieramy wpis, żeby znać ID pojazdu do aktualizacji statystyk
        val serviceHistory = serviceHistoryRepository.findById(id)

        if (serviceHistory != null) {
            val deleted = serviceHistoryRepository.deleteById(id)
            if (deleted) {
                // Aktualizujemy statystyki pojazdu po usunięciu wpisu
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

    // Metoda do aktualizacji statystyk serwisowych pojazdu
    private fun updateVehicleServiceStats(vehicleId: VehicleId) {
        logger.debug("Updating service statistics for vehicle: ${vehicleId.value}")

        val vehicle = vehicleRepository.findById(vehicleId) ?: return
        val serviceHistory = serviceHistoryRepository.findByVehicleId(vehicleId)

        if (serviceHistory.isEmpty()) {
            // Brak historii serwisowej, reset statystyk
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

        // Obliczanie statystyk
        val totalServices = serviceHistory.size
        val totalSpent = serviceHistory.sumOf { it.price }
        val lastServiceDate = serviceHistory
            .maxByOrNull { it.date }
            ?.date?.atStartOfDay() // Konwersja LocalDate na LocalDateTime

        // Aktualizacja pojazdu
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

    // Walidacja pojazdu
    private fun validateVehicle(vehicle: Vehicle) {
        // Sprawdzamy, czy marka i model nie są puste
        if (vehicle.make.isBlank()) {
            throw IllegalArgumentException("Vehicle make cannot be empty")
        }

        if (vehicle.model.isBlank()) {
            throw IllegalArgumentException("Vehicle model cannot be empty")
        }

        // Sprawdzamy, czy numer rejestracyjny nie jest pusty
        if (vehicle.licensePlate.isBlank()) {
            throw IllegalArgumentException("License plate cannot be empty")
        }

        // Sprawdzamy, czy rok produkcji jest prawidłowy
        val currentYear = LocalDate.now().year
        if (vehicle.year < 1900 || vehicle.year > currentYear + 1) {
            throw IllegalArgumentException("Invalid production year. Year must be between 1900 and ${currentYear + 1}")
        }

        // Sprawdzamy, czy lista właścicieli nie jest pusta
        if (vehicle.ownerIds.isEmpty()) {
            throw IllegalArgumentException("Vehicle must have at least one owner")
        }
    }

    // Walidacja wpisu historii serwisowej
    private fun validateServiceHistory(serviceHistory: ServiceHistory) {
        // Sprawdzamy, czy opis nie jest pusty
        if (serviceHistory.description.isBlank()) {
            throw IllegalArgumentException("Service history description cannot be empty")
        }

        // Sprawdzamy, czy typ usługi nie jest pusty
        if (serviceHistory.serviceType.isBlank()) {
            throw IllegalArgumentException("Service type cannot be empty")
        }

        // Sprawdzamy, czy cena jest dodatnia
        if (serviceHistory.price < 0) {
            throw IllegalArgumentException("Service price cannot be negative")
        }

        // Sprawdzamy, czy data nie jest z przyszłości
        if (serviceHistory.date.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("Service date cannot be in the future")
        }

        // Sprawdzamy, czy pojazd istnieje
        val vehicle = vehicleRepository.findById(serviceHistory.vehicleId)
            ?: throw IllegalArgumentException("Vehicle with ID ${serviceHistory.vehicleId.value} not found")
    }
}