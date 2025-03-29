package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolClientModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolVehicleModel
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.repository.InMemoryClientVehicleAssociationRepository
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
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val clientVehicleAssociationRepository: InMemoryClientVehicleAssociationRepository
) {
    private val logger = LoggerFactory.getLogger(CarReceptionService::class.java)

    fun createProtocol(protocol: CreateProtocolRootModel): ProtocolId {
        logger.info("Creating new protocol for vehicle: ${protocol.vehicle.brand} ${protocol.vehicle.model} (${protocol.vehicle.licensePlate})")

        val client = findOrCreateClient(protocol.client)
        val vehicle = findExistingVehicle(protocol.vehicle) ?: createNewVehicle(protocol.vehicle)
            .also { clientVehicleAssociationRepository.newAssociation(it.id, client.id) }
            .also { initializeVehicleStatistics(it.id) }
            .also { incrementClientVehicles(client.id) }

        val protocolWithFilledIds = protocol.copy(
            client = protocol.client.copy(
                id = client.id.value.toString()
            ),
            vehicle = protocol.vehicle.copy(
                id = vehicle.id.value.toString()
            )
        )

        val savedProtocolId = carReceptionRepository.save(protocolWithFilledIds)

        logger.info("Created protocol with ID: ${savedProtocolId}")
        return savedProtocolId
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
        clientId: Long? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters: clientName=$clientName, clientId=$clientId, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate")

        var result = carReceptionRepository.findAll()

        if (!clientName.isNullOrBlank()) {
            result = result.filter {
                it.client.name.contains(clientName, ignoreCase = true) ||
                        it.client.companyName?.contains(clientName, ignoreCase = true) == true
            }
        }

        if (clientId != null) {
            result = result.filter {
                it.client.id == clientId
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

    private fun findOrCreateClient(clientData: CreateProtocolClientModel): ClientDetails {
        // Jeśli klient ma ID, próbujemy go znaleźć
        if (clientData.id != null) {
            clientRepository.findById(ClientId(clientData.id.toLong()))?.let {
                logger.debug("Found existing client with ID: ${clientData.id}")
                return it
            }
        }

        // Próbujemy znaleźć klienta po danych kontaktowych
        val existingClient = findClientByContactInfo(clientData.email, clientData.phone)
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

    private fun findClientByContactInfo(email: String?, phoneNumber: String?): ClientDetails? {
        if (email.isNullOrBlank() && phoneNumber.isNullOrBlank()) {
            return null
        }

        return clientRepository.findClient(email, phoneNumber)
    }

    private fun createNewClient(client: CreateProtocolClientModel): ClientDetails {
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

    private fun findExistingVehicle(protocol: CreateProtocolVehicleModel): Vehicle? {
        return vehicleRepository.findByVinOrLicensePlate(
            protocol.vin,
            protocol.licensePlate
        )
    }

    private fun createNewVehicle(vehicle: CreateProtocolVehicleModel): Vehicle {
        val newVehicle = Vehicle(
            id = VehicleId.generate(),
            make = vehicle.brand,
            model = vehicle.model,
            year = vehicle.productionYear,
            licensePlate = vehicle.licensePlate,
            color = vehicle.color,
            vin = vehicle.vin,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        return vehicleRepository.save(newVehicle)
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

    private fun incrementClientVehicles(clientId: ClientId) {
        val clientStats = clientStatisticsRepository.findById(clientId)
            ?: ClientStats(clientId.value, 0, "0".toBigDecimal(), 0)

        val updatedStats = clientStats.copy(
            vehiclesNo = clientStats.vehiclesNo + 1
        )

        clientStatisticsRepository.save(updatedStats)
    }
}