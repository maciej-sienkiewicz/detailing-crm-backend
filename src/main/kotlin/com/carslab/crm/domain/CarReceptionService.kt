package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CarReceptionService(
    private val carReceptionRepository: CarReceptionRepository,
    private val clientRepository: ClientRepository,
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(CarReceptionService::class.java)

    fun createProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Creating new protocol for vehicle: ${protocol.vehicle.make} ${protocol.vehicle.model} (${protocol.vehicle.licensePlate})")

        // Logika związana z klientem - znajdź lub utwórz
        val client = findOrCreateClient(protocol.client)

        // Stwórz protokół z poprawnym ID klienta
        val protocolWithClientId = protocol.copy(
            client = protocol.client.copy(
                id = client.id.value
            )
        )

        // Zapisz protokół
        val savedProtocol = carReceptionRepository.save(protocolWithClientId)

        // Logika związana z pojazdem - znajdź lub utwórz
        val vehicle = findOrCreateVehicle(savedProtocol)

        logger.info("Created protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Updating protocol with ID: ${protocol.id.value}")

        val existingProtocol = carReceptionRepository.findById(protocol.id)
            ?: throw ResourceNotFoundException("Protocol", protocol.id.value)

        // Aktualizujemy informacje audytowe
        val updatedProtocol = protocol.copy(
            audit = protocol.audit.copy(
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = if (protocol.status != existingProtocol.status) {
                    LocalDateTime.now()
                } else {
                    existingProtocol.audit.statusUpdatedAt
                }
            )
        )

        // Zapisujemy zaktualizowany protokół
        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Updated protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        logger.info("Changing status of protocol ${protocolId.value} to $newStatus")

        val existingProtocol = carReceptionRepository.findById(protocolId)
            ?: throw ResourceNotFoundException("Protocol", protocolId.value)

        val now = LocalDateTime.now()
        val updatedProtocol = existingProtocol.copy(
            status = newStatus,
            audit = existingProtocol.audit.copy(
                updatedAt = now,
                statusUpdatedAt = now
            )
        )

        if (updatedProtocol.status == ProtocolStatus.COMPLETED) {
            updateStatisticsOnCompletion(updatedProtocol)
        }

        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Status of protocol ${savedProtocol.id.value} changed to ${savedProtocol.status}")
        return savedProtocol
    }

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        logger.debug("Getting protocol by ID: ${protocolId.value}")
        return carReceptionRepository.findById(protocolId)
    }

    fun getAllProtocols(): List<CarReceptionProtocol> {
        logger.debug("Getting all protocols")
        return carReceptionRepository.findAll()
    }

    fun searchProtocols(
        clientName: String? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters: clientName=$clientName, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate")

        var result = carReceptionRepository.findAll()

        if (!clientName.isNullOrBlank()) {
            result = result.filter {
                it.client.name.contains(clientName, ignoreCase = true) ||
                        it.client.companyName?.contains(clientName, ignoreCase = true) == true
            }
        }

        if (!licensePlate.isNullOrBlank()) {
            result = result.filter {
                it.vehicle.licensePlate.contains(licensePlate, ignoreCase = true)
            }
        }

        if (status != null) {
            result = result.filter { it.status == status }
        }

        if (startDate != null) {
            result = result.filter {
                !it.period.endDate.isBefore(startDate)
            }
        }

        if (endDate != null) {
            result = result.filter {
                !it.period.startDate.isAfter(endDate)
            }
        }

        logger.debug("Found ${result.size} protocols matching filters")
        return result
    }

    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        logger.info("Deleting protocol with ID: ${protocolId.value}")
        return carReceptionRepository.deleteById(protocolId)
    }

    private fun findOrCreateClient(clientData: Client): ClientDetails {
        // Jeśli klient ma ID, próbujemy go znaleźć
        if (clientData.id != null) {
            clientRepository.findById(ClientId(clientData.id))?.let {
                logger.debug("Found existing client with ID: ${clientData.id}")
                return it
            }
        }

        // Próbujemy znaleźć klienta po danych kontaktowych
        val existingClient = findClientByContactInfo(clientData)
        if (existingClient != null) {
            logger.debug("Found existing client by contact information")
            return existingClient
        }

        // Jeśli nie znaleźliśmy klienta, tworzymy nowego
        logger.info("Creating new client: ${clientData.name}")
        return createNewClient(clientData)
    }

    private fun findClientByContactInfo(client: Client): ClientDetails? {
        if (client.email.isNullOrBlank() && client.phone.isNullOrBlank()) {
            return null
        }

        return clientRepository.findClient(client)
    }

    private fun createNewClient(client: Client): ClientDetails {
        val nameParts = client.name.split(" ")
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

        val newClient = ClientDetails(
            id = ClientId(),
            firstName = firstName,
            lastName = lastName,
            email = client.email ?: "",
            phone = client.phone ?: "",
            company = client.companyName,
            taxId = client.taxId,
        )

        val savedClient = clientRepository.save(newClient)

        // Inicjalizacja statystyk klienta
        initializeClientStatistics(savedClient.id)

        return savedClient
    }

    private fun findOrCreateVehicle(protocol: CarReceptionProtocol): Vehicle {
        // Próbujemy znaleźć pojazd po numerze VIN lub numerze rejestracyjnym
        val existingVehicle = vehicleRepository.findByVinOrLicensePlate(
            protocol.vehicle.vin,
            protocol.vehicle.licensePlate
        )

        if (existingVehicle != null) {
            logger.debug("Found existing vehicle with license plate: ${protocol.vehicle.licensePlate}")

            // Sprawdzamy czy klient jest już właścicielem pojazdu
            if (protocol.client.id != null && !existingVehicle.ownerIds.contains(protocol.client.id.toString())) {
                // Jeśli nie, dodajemy go jako właściciela
                logger.info("Adding client ${protocol.client.id} as owner to vehicle ${existingVehicle.id.value}")
                return addOwnerToVehicle(existingVehicle, protocol.client.id.toString())
            }

            return existingVehicle
        }

        // Jeśli nie znaleźliśmy pojazdu, tworzymy nowy
        logger.info("Creating new vehicle: ${protocol.vehicle.make} ${protocol.vehicle.model} (${protocol.vehicle.licensePlate})")
        return createNewVehicle(protocol)
    }

    private fun createNewVehicle(protocol: CarReceptionProtocol): Vehicle {
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

        // Inicjalizacja statystyk pojazdu
        initializeVehicleStatistics(savedVehicle.id)

        // Aktualizacja statystyk klienta jeśli istnieje ID klienta
        protocol.client.id?.let { updateClientVehiclesStatistic(it) }

        return savedVehicle
    }

    private fun addOwnerToVehicle(vehicle: Vehicle, ownerId: String): Vehicle {
        val updatedVehicle = vehicle.copy(
            ownerIds = vehicle.ownerIds + ownerId,
            audit = vehicle.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        val savedVehicle = vehicleRepository.save(updatedVehicle)

        // Aktualizuj statystyki klienta
        try {
            updateClientVehiclesStatistic(ownerId.toLong())
        } catch (e: NumberFormatException) {
            logger.warn("Invalid owner ID format: $ownerId")
        }

        return savedVehicle
    }

    private fun updateStatisticsOnCompletion(protocol: CarReceptionProtocol) {
        // Aktualizacja statystyk klienta
        protocol.client.id?.let { clientId ->
            val clientStats = clientStatisticsRepository.findById(ClientId(clientId))
                ?: ClientStats(clientId, 0, "0".toBigDecimal(), 0)

            val updatedClientStats = clientStats.copy(
                visitNo = clientStats.visitNo + 1,
                gmv = clientStats.gmv + protocol.services.sumOf { it.finalPrice.amount }.toBigDecimal()
            )

            clientStatisticsRepository.save(updatedClientStats)
        }

        // Aktualizacja statystyk pojazdu
        val vehicle = vehicleRepository.findByVinOrLicensePlate(protocol.vehicle.vin, protocol.vehicle.licensePlate)
        vehicle?.let {
            val vehicleStats = vehicleStatisticsRepository.findById(it.id)
            val updatedStats = vehicleStats.copy(
                visitNo = vehicleStats.visitNo + 1,
                gmv = vehicleStats.gmv + protocol.services.sumOf { it.finalPrice.amount }.toBigDecimal()
            )

            vehicleStatisticsRepository.save(updatedStats)
        }
    }

    private fun initializeClientStatistics(clientId: ClientId) {
        val stats = ClientStats(
            clientId = clientId.value,
            visitNo = 0,
            gmv = "0".toBigDecimal(),
            vehiclesNo = 0
        )
        clientStatisticsRepository.save(stats)
    }

    private fun initializeVehicleStatistics(vehicleId: VehicleId) {
        val stats = com.carslab.crm.domain.model.stats.VehicleStats(
            vehicleId = vehicleId.value,
            visitNo = 0,
            gmv = "0".toBigDecimal()
        )
        vehicleStatisticsRepository.save(stats)
    }

    private fun updateClientVehiclesStatistic(clientId: Long) {
        val clientStats = clientStatisticsRepository.findById(ClientId(clientId))
            ?: ClientStats(clientId, 0, "0".toBigDecimal(), 0)

        val updatedStats = clientStats.copy(
            vehiclesNo = clientStats.vehiclesNo + 1
        )

        clientStatisticsRepository.save(updatedStats)
    }
}