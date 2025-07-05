package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.CreateProtocolCommand
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.services.ProtocolDomainService
import com.carslab.crm.modules.clients.domain.ClientApplicationService
import com.carslab.crm.modules.clients.domain.VehicleApplicationService
import com.carslab.crm.modules.clients.domain.ClientVehicleAssociationService
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleRelationshipType
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.infrastructure.events.EventPublisher
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.clients.domain.ClientDetailResponse
import com.carslab.crm.modules.clients.domain.CreateClientRequest
import com.carslab.crm.modules.clients.domain.CreateVehicleRequest
import com.carslab.crm.modules.clients.domain.VehicleDetailResponse
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateClientCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateVehicleCommand
import com.carslab.crm.modules.visits.domain.events.*
import com.carslab.crm.domain.model.ProtocolStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Service
class EnhancedCreateProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationService: VehicleApplicationService,
    private val clientVehicleAssociationService: ClientVehicleAssociationService,
    private val eventPublisher: EventPublisher,
    private val securityContext: SecurityContext,
) : CommandHandler<CreateProtocolCommand, String> {

    private val logger = LoggerFactory.getLogger(EnhancedCreateProtocolCommandHandler::class.java)

    @Transactional
    override fun handle(command: CreateProtocolCommand): String {
        logger.info("Creating protocol for vehicle: ${command.vehicle.brand} ${command.vehicle.model}")

        val client = findOrCreateClient(command.client)
        val vehicle = findOrCreateVehicle(command.vehicle)

        ensureClientVehicleAssociation(client.id, vehicle.id)

        val protocolModel = protocolDomainService.createProtocol(
            command.copy(
                client = command.client.copy(id = client.id.toString()),
                vehicle = command.vehicle.copy(id = vehicle.id.toString())
            )
        )

        val savedProtocolId = protocolRepository.save(protocolModel)

        // Publish comprehensive creation event
        publishProtocolCreationEvents(command, savedProtocolId.value, client, vehicle)

        logger.info("Successfully created protocol: ${savedProtocolId.value}")
        return savedProtocolId.value
    }

    private fun publishProtocolCreationEvents(
        command: CreateProtocolCommand,
        protocolId: String,
        client: ClientDetailResponse,
        vehicle: VehicleDetailResponse
    ) {
        val totalAmount = command.services.sumOf { it.finalPrice ?: it.basePrice }

        // Main protocol creation event
        eventPublisher.publish(
            ProtocolCreatedEvent(
                protocolId = protocolId,
                protocolTitle = command.title,
                clientId = client.id.toString(),
                clientName = client.fullName,
                vehicleId = vehicle.id.toString(),
                vehicleMake = vehicle.make,
                vehicleModel = vehicle.model,
                licensePlate = vehicle.licensePlate,
                status = command.status.name,
                servicesCount = command.services.size,
                totalAmount = totalAmount,
                companyId = securityContext.getCurrentCompanyId(),
                userId = securityContext.getCurrentUserId(),
                userName = securityContext.getCurrentUserName()
            )
        )

        // Status-specific events
        when (command.status) {
            ProtocolStatus.SCHEDULED -> {
                eventPublisher.publish(
                    VisitScheduledEvent(
                        visitId = protocolId,
                        visitTitle = command.title,
                        clientId = client.id.toString(),
                        clientName = client.fullName,
                        vehicleId = vehicle.id.toString(),
                        vehicleMake = vehicle.make,
                        vehicleModel = vehicle.model,
                        licensePlate = vehicle.licensePlate,
                        scheduledDate = command.period.startDate.toString(),
                        services = command.services.map { it.name },
                        companyId = securityContext.getCurrentCompanyId(),
                        userId = securityContext.getCurrentUserId(),
                        userName = securityContext.getCurrentUserName()
                    )
                )
            }
            ProtocolStatus.IN_PROGRESS -> {
                eventPublisher.publish(
                    ProtocolWorkStartedEvent(
                        protocolId = protocolId,
                        protocolTitle = command.title,
                        assignedTechnicians = listOf(securityContext.getCurrentUserName() ?: "Unknown"),
                        estimatedCompletionTime = command.period.endDate.toString(),
                        plannedServices = command.services.map { it.name },
                        companyId = securityContext.getCurrentCompanyId(),
                        userId = securityContext.getCurrentUserId(),
                        userName = securityContext.getCurrentUserName(),
                        clientId = client.id.toString(),
                        clientName = client.fullName,
                        vehicleId = vehicle.id.toString(),
                        vehicleDisplayName = "${vehicle.make} ${vehicle.model} (${vehicle.licensePlate})".trim()
                    )
                )
            }
            else -> {
                // Log other statuses but don't emit specific events
                logger.debug("Protocol created with status: ${command.status}")
            }
        }

        // Services-related event if services are present
        if (command.services.isNotEmpty()) {
            eventPublisher.publish(
                ProtocolServicesUpdatedEvent(
                    protocolId = protocolId,
                    servicesCount = command.services.size,
                    totalAmount = totalAmount,
                    changedServices = command.services.map { it.name },
                    companyId = securityContext.getCurrentCompanyId(),
                    userId = securityContext.getCurrentUserId(),
                    userName = securityContext.getCurrentUserName()
                )
            )
        }
    }

    private fun findOrCreateClient(clientCommand: CreateClientCommand): ClientDetailResponse {
        clientCommand.id?.toLongOrNull()?.let { id ->
            clientApplicationService.getClientById(id)?.let {
                logger.debug("Found existing client by ID: $id")
                return it
            }
        }

        val existingClient = findClientByContactInfo(clientCommand.email, clientCommand.phone)
        if (existingClient != null) {
            logger.debug("Found existing client by contact info: ${existingClient.email}")
            return existingClient
        }

        logger.info("Creating new client: ${clientCommand.name}")
        return createNewClient(clientCommand)
    }

    private fun findOrCreateVehicle(vehicleCommand: CreateVehicleCommand): VehicleDetailResponse {
        vehicleCommand.id?.toLongOrNull()?.let { id ->
            vehicleApplicationService.getVehicleById(id)?.let {
                logger.debug("Found existing vehicle by ID: $id")
                return it
            }
        }

        val existingVehicle = findVehicleByIdentifiers(
            vin = vehicleCommand.vin,
            licensePlate = vehicleCommand.licensePlate
        )
        if (existingVehicle != null) {
            logger.debug("Found existing vehicle: ${existingVehicle.licensePlate}")
            return existingVehicle
        }

        logger.info("Creating new vehicle: ${vehicleCommand.brand} ${vehicleCommand.model}")
        return createNewVehicle(vehicleCommand)
    }

    private fun findClientByContactInfo(email: String?, phone: String?): ClientDetailResponse? {
        if (email.isNullOrBlank() && phone.isNullOrBlank()) {
            return null
        }

        return try {
            val searchResults = clientApplicationService.searchClients(
                email = email,
                phone = phone,
                pageable = PageRequest.of(0, 1)
            )
            searchResults.content.firstOrNull()
        } catch (e: Exception) {
            logger.warn("Error searching for client by contact info", e)
            null
        }
    }

    private fun findVehicleByIdentifiers(vin: String?, licensePlate: String?): VehicleDetailResponse? {
        if (vin.isNullOrBlank() && licensePlate.isNullOrBlank()) {
            return null
        }

        return try {
            val searchResults = vehicleApplicationService.searchVehicles(
                vin = vin,
                licensePlate = licensePlate,
                pageable = PageRequest.of(0, 1)
            )
            searchResults.content.firstOrNull()
        } catch (e: Exception) {
            logger.warn("Error searching for vehicle by identifiers", e)
            null
        }
    }

    private fun createNewClient(clientCommand: CreateClientCommand): ClientDetailResponse {
        val nameParts = clientCommand.name.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        val createRequest = CreateClientRequest(
            firstName = firstName,
            lastName = lastName,
            email = clientCommand.email ?: "",
            phone = clientCommand.phone ?: "",
            company = clientCommand.companyName,
            taxId = clientCommand.taxId
        )

        return try {
            clientApplicationService.createClient(createRequest)
        } catch (e: Exception) {
            logger.warn("Failed to create client, trying to find existing one", e)
            findClientByContactInfo(clientCommand.email, clientCommand.phone)
                ?: throw IllegalStateException("Could not create or find client: ${clientCommand.name}", e)
        }
    }

    private fun createNewVehicle(vehicleCommand: CreateVehicleCommand): VehicleDetailResponse {
        val createRequest = CreateVehicleRequest(
            make = vehicleCommand.brand ?: "",
            model = vehicleCommand.model ?: "",
            year = vehicleCommand.productionYear,
            licensePlate = vehicleCommand.licensePlate ?: "",
            color = vehicleCommand.color,
            vin = vehicleCommand.vin,
            mileage = vehicleCommand.mileage,
            ownerIds = emptyList()
        )

        return try {
            vehicleApplicationService.createVehicle(createRequest)
        } catch (e: Exception) {
            logger.warn("Failed to create vehicle, trying to find existing one", e)
            findVehicleByIdentifiers(vehicleCommand.vin, vehicleCommand.licensePlate)
                ?: throw IllegalStateException("Could not create or find vehicle: ${vehicleCommand.licensePlate}", e)
        }
    }

    private fun ensureClientVehicleAssociation(clientId: Long, vehicleId: Long) {
        try {
            val existingAssociations = clientVehicleAssociationService.getClientVehicles(ClientId.of(clientId))
            val alreadyAssociated = existingAssociations.any { it.id.value == vehicleId }

            if (!alreadyAssociated) {
                logger.debug("Creating client-vehicle association: client=$clientId, vehicle=$vehicleId")
                clientVehicleAssociationService.associateClientWithVehicle(
                    ClientId.of(clientId),
                    VehicleId.of(vehicleId),
                    VehicleRelationshipType.OWNER
                )
            }
        } catch (e: Exception) {
            logger.warn("Could not create client-vehicle association (might already exist)", e)
        }
    }
}
