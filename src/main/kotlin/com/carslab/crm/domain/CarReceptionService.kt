package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolClientModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolVehicleModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.domain.port.*
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.repository.InMemoryClientVehicleAssociationRepository
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService
import com.carslab.crm.infrastructure.repository.InMemoryProtocolCommentsRepository
import com.carslab.crm.infrastructure.repository.InMemoryProtocolServicesRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime

@Service
class CarReceptionService(
    private val carReceptionRepository: CarReceptionRepository,
    private val clientRepository: ClientRepository,
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val clientVehicleAssociationRepository: InMemoryClientVehicleAssociationRepository,
    private val protocolServicesRepository: InMemoryProtocolServicesRepository,
    private val imageStorageService: InMemoryImageStorageService,
    private val inMemoryProtocolCommentsRepository: InMemoryProtocolCommentsRepository,
    ) {
    private val logger = LoggerFactory.getLogger(CarReceptionService::class.java)

    fun createProtocol(createProtocolCommand: CreateProtocolRootModel): ProtocolId {
        logger.info("Creating new protocol for vehicle: ${createProtocolCommand.vehicle.brand} ${createProtocolCommand.vehicle.model} (${createProtocolCommand.vehicle.licensePlate})")

        val client = findOrCreateClient(createProtocolCommand.client)
        //TODO: Jak samochod istnieje i nie ma przypisanego klienta to go dodaj
        val vehicle = findExistingVehicle(createProtocolCommand.vehicle) ?: createNewVehicle(createProtocolCommand.vehicle)
            .also { clientVehicleAssociationRepository.newAssociation(it.id, client.id) }
            .also { initializeVehicleStatistics(it.id) }
            .also { incrementClientVehicles(client.id) }

        val protocolWithFilledIds = createProtocolCommand.copy(
            client = createProtocolCommand.client.copy(
                id = client.id.value.toString()
            ),
            vehicle = createProtocolCommand.vehicle.copy(
                id = vehicle.id.value.toString()
            )
        )

        val savedProtocolId = carReceptionRepository.save(protocolWithFilledIds)
        protocolServicesRepository.saveServices(createProtocolCommand.services, savedProtocolId)
        updateStatisticsOnCreateComponent(protocolWithFilledIds)

        logger.info("Created protocol with ID: ${savedProtocolId}")
        return savedProtocolId
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Updating protocol with ID: ${protocol.id.value}")

        val existingProtocol = carReceptionRepository.findById(protocol.id)
            ?.enrichProtocol()
            ?: throw ResourceNotFoundException("Protocol", protocol.id.value)

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
        
        protocolServicesRepository
            .saveServices(
                services = updatedProtocol.protocolServices
                    .map {
                        CreateServiceModel(
                            name = it.name,
                            basePrice = it.basePrice,
                            discount = it.discount,
                            finalPrice = it.finalPrice,
                            approvalStatus = it.approvalStatus,
                            note = it.note,
                            quantity = it.quantity
                        )
                    },
                protocolId = updatedProtocol.id
            )

        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Updated protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        logger.info("Changing status of protocol ${protocolId.value} to $newStatus")

        val existingProtocol = carReceptionRepository.findById(protocolId)
            ?.enrichProtocol()
            ?: throw ResourceNotFoundException("Protocol", protocolId.value)

        val now = LocalDateTime.now()
        commentOnStatusChange(newStatus, existingProtocol.status, protocolId)
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
        return carReceptionRepository.findById(protocolId)?.enrichProtocol()
    }

    private fun commentOnStatusChange(newStatus: ProtocolStatus, previousStatus: ProtocolStatus, protocolId: ProtocolId) =
        inMemoryProtocolCommentsRepository.save(ProtocolComment(
            protocolId = protocolId,
            author = "Administrator",
            content = "Zmieniono status protokołu z: $previousStatus na: $newStatus",
            timestamp = Instant.now().toString(),
            type = "system"
        ))

    private fun ProtocolView.enrichProtocol(): CarReceptionProtocol {
        val vehicle = vehicleRepository.findById(vehicleId)
        val client = clientRepository.findById(clientId)
        val services = protocolServicesRepository.findByIds(id)
        val images = imageStorageService.getImagesByProtocol(id)

        return CarReceptionProtocol(
            id = id,
            title = title,
            vehicle = vehicle.let {
                VehicleDetails(
                    make = it?.make ?: "",
                    model = it?.model ?: "",
                    licensePlate = it?.licensePlate ?: "",
                    productionYear = it?.year ?: 0,
                    vin = it?.vin ?: "",
                    color = it?.color ?: "",
                    mileage = it?.mileage
                )
            },
            client = client.let {
                Client(
                    id = it?.id?.value,
                    name = "${it?.firstName} ${it?.lastName}",
                    email = it?.email,
                    phone = it?.phone,
                    companyName = it?.company,
                    taxId = it?.taxId
                )
            },
            period = period,
            status = status,
            protocolServices = services
                .map {
                    ProtocolService(
                        id = it.id.toString(),
                        name = it.name,
                        basePrice = it.basePrice,
                        discount = it.discount,
                        finalPrice = it.finalPrice,
                        approvalStatus = it.approvalStatus,
                        note = it.note,
                        quantity = it.quantity
                    )
                },
            notes = notes,
            referralSource = ReferralSource.SEARCH_ENGINE,
            otherSourceDetails = "",
            audit = AuditInfo(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = LocalDateTime.now(),
                createdBy = LocalDateTime.now().toString(),
                updatedBy = LocalDateTime.now().toString(),
                appointmentId = ""
            ),
            mediaItems = images.map {
                MediaItem(
                    id = it.id,
                    type = MediaType.PHOTO,
                    name = it.name,
                    size = it.size,
                    description = "Zamockowany opis",
                    createdAt = LocalDateTime.now(),
                    tags = it.tags
                )
            }
        )
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
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): List<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters: clientName=$clientName, clientId=$clientId, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate")

        val baseProtocols = carReceptionRepository.searchProtocols(
            clientName = clientName,
            clientId = clientId,
            licensePlate = licensePlate,
            status = status,
            startDate = startDate,
            endDate = endDate
        )
        val vehiclesIds = baseProtocols.map { it.vehicleId }
        val clientIds = baseProtocols.map { it.clientId }

        val vehicles: Map<VehicleId, Vehicle> = vehicleRepository.findByIds(vehiclesIds).associateBy { it.id }
        val clients = clientRepository.findByIds(clientIds).associateBy { it.id }

        return baseProtocols.map {
            CarReceptionProtocol(
                id = it.id,
                title = it.title,
                vehicle = vehicles[it.vehicleId]!!.let {
                    VehicleDetails(
                        make = it.make ?: "",
                        model = it.model ?: "",
                        licensePlate = it.model ?: "",
                        productionYear = it.year ?: 0,
                        vin = it.vin ?: "",
                        color = it.color ?: "",
                        mileage = 0
                    )
                },
                client = clients[it.clientId]!!.let {
                    Client(
                        id = it.id.value,
                        name = "${it.firstName} ${it.lastName}",
                        email = it.email,
                        phone = it.phone,
                        companyName = it.company,
                        taxId = it.taxId
                    )
                },
                period = it.period,
                status = it.status,
                protocolServices = emptyList(),
                notes = it.notes,
                referralSource = ReferralSource.SEARCH_ENGINE,
                otherSourceDetails = "",
                audit = AuditInfo(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    statusUpdatedAt = LocalDateTime.now(),
                    createdBy = LocalDateTime.now().toString(),
                    updatedBy = LocalDateTime.now().toString(),
                    appointmentId = ""
                )
            )
        }
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
            mileage = vehicle.mileage,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        return vehicleRepository.save(newVehicle)
    }

    private fun updateStatisticsOnCreateComponent(protocol: CreateProtocolRootModel) {
        // Aktualizacja statystyk klienta
        protocol.client.id?.let { clientId ->
            val clientStats = clientStatisticsRepository.findById(ClientId(clientId.toLong()))
                ?: ClientStats(clientId.toLong(), 0, "0".toBigDecimal(), 0)

            val updatedClientStats = clientStats.copy(
                visitNo = clientStats.visitNo + 1,
            )

            clientStatisticsRepository.save(updatedClientStats)
        }

        // Aktualizacja statystyk pojazdu
        val vehicle = vehicleRepository.findByVinOrLicensePlate(protocol.vehicle.vin, protocol.vehicle.licensePlate)
        vehicle?.let {
            val vehicleStats = vehicleStatisticsRepository.findById(it.id)
            val updatedStats = vehicleStats.copy(
                visitNo = vehicleStats.visitNo + 1,
            )

            vehicleStatisticsRepository.save(updatedStats)
        }
    }

    private fun updateStatisticsOnCompletion(protocol: CarReceptionProtocol) {
        // Aktualizacja statystyk klienta
        protocol.client.id?.let { clientId ->
            val clientStats = clientStatisticsRepository.findById(ClientId(clientId))
                ?: ClientStats(clientId, 0, "0".toBigDecimal(), 0)

            val updatedClientStats = clientStats.copy(
                gmv = clientStats.gmv + protocol.protocolServices.sumOf { it.finalPrice.amount }.toBigDecimal()
            )

            clientStatisticsRepository.save(updatedClientStats)
        }

        // Aktualizacja statystyk pojazdu
        val vehicle = vehicleRepository.findByVinOrLicensePlate(protocol.vehicle.vin, protocol.vehicle.licensePlate)
        vehicle?.let {
            val vehicleStats = vehicleStatisticsRepository.findById(it.id)
            val updatedStats = vehicleStats.copy(
                gmv = vehicleStats.gmv + protocol.protocolServices.sumOf { it.finalPrice.amount }.toBigDecimal()
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
        val stats = VehicleStats(
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

    fun updateServices(protocolId: ProtocolId, services: List<CreateServiceModel>) {
        protocolServicesRepository.saveServices(services, protocolId)
    }
}