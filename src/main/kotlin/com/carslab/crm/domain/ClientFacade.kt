package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.ContactAttemptRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClientService(
    private val clientRepository: ClientRepository,
    private val contactAttemptRepository: ContactAttemptRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(ClientService::class.java)

    fun createClient(client: ClientDetails): ClientDetails {
        logger.info("Creating new client: ${client.fullName}")
        validateClient(client)
        val savedClient = clientRepository.save(client)
        initializeClientStatistics(savedClient.id)
        logger.info("Created client with ID: ${savedClient.id.value}")
        return savedClient
    }

    fun updateClient(client: ClientDetails): ClientDetails {
        logger.info("Updating client with ID: ${client.id.value}")
        val existingClient = clientRepository.findById(client.id)
            ?: throw ResourceNotFoundException("Client", client.id.value)

        validateClient(client)
        val updatedClient = client.copy(
            audit = client.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        val savedClient = clientRepository.save(updatedClient)
        logger.info("Updated client with ID: ${savedClient.id.value}")
        return savedClient
    }

    fun getClientById(clientId: ClientId): ClientDetails? {
        logger.debug("Getting client by ID: ${clientId.value}")
        return clientRepository.findById(clientId)
    }

    fun getAllClients(): List<ClientDetails> {
        logger.debug("Getting all clients")
        return clientRepository.findAll()
    }

    fun searchClients(
        name: String? = null,
        email: String? = null,
        phone: String? = null
    ): List<ClientDetails> {
        logger.debug("Searching clients with filters: name=$name, email=$email, phone=$phone")

        var result = clientRepository.findAll()

        if (!name.isNullOrBlank()) {
            result = result.filter {
                it.fullName.contains(name, ignoreCase = true) ||
                        it.firstName.contains(name, ignoreCase = true) ||
                        it.lastName.contains(name, ignoreCase = true)
            }
        }

        if (!email.isNullOrBlank()) {
            result = result.filter {
                it.email.contains(email, ignoreCase = true)
            }
        }

        if (!phone.isNullOrBlank()) {
            result = result.filter {
                it.phone.replace(" ", "").contains(phone.replace(" ", ""))
            }
        }

        logger.debug("Found ${result.size} clients matching filters")
        return result
    }

    fun deleteClient(clientId: ClientId): Boolean {
        logger.info("Deleting client with ID: ${clientId.value}")
        return clientRepository.deleteById(clientId)
    }

    fun createContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        logger.info("Creating new contact attempt for client: ${contactAttempt.clientId}")
        validateContactAttempt(contactAttempt)
        val savedContactAttempt = contactAttemptRepository.save(contactAttempt)
        logger.info("Created contact attempt with ID: ${savedContactAttempt.id.value}")
        return savedContactAttempt
    }

    fun updateContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        logger.info("Updating contact attempt with ID: ${contactAttempt.id.value}")
        val existingContactAttempt = contactAttemptRepository.findById(contactAttempt.id)
            ?: throw ResourceNotFoundException("Contact attempt", contactAttempt.id.value)

        validateContactAttempt(contactAttempt)
        val updatedContactAttempt = contactAttempt.copy(
            audit = contactAttempt.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        val savedContactAttempt = contactAttemptRepository.save(updatedContactAttempt)
        logger.info("Updated contact attempt with ID: ${savedContactAttempt.id.value}")
        return savedContactAttempt
    }

    fun getContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        logger.debug("Getting contact attempts for client: $clientId")
        return contactAttemptRepository.findContactAttemptsByClientId(clientId)
    }

    fun getContactAttemptById(contactAttemptId: ContactAttemptId): ContactAttempt? {
        logger.debug("Getting contact attempt by ID: ${contactAttemptId.value}")
        return contactAttemptRepository.findById(contactAttemptId)
    }

    fun deleteContactAttempt(contactAttemptId: ContactAttemptId): Boolean {
        logger.info("Deleting contact attempt with ID: ${contactAttemptId.value}")
        return contactAttemptRepository.deleteById(contactAttemptId)
    }

    fun getClientStatistics(clientId: ClientId): ClientStats {
        return clientStatisticsRepository.findById(clientId)
            ?: ClientStats(clientId.value, 0, "0".toBigDecimal(), 0)
    }

    private fun validateContactAttempt(contactAttempt: ContactAttempt) {
        if (contactAttempt.description.isBlank()) {
            throw ValidationException("Contact attempt description cannot be empty")
        }

        if (contactAttempt.date > LocalDateTime.now()) {
            throw ValidationException("Contact attempt date cannot be in the future")
        }

        if (contactAttempt.clientId.isBlank()) {
            throw ValidationException("Client ID cannot be empty")
        }
    }

    private fun validateClient(client: ClientDetails) {
        if (!client.hasValidContactInfo()) {
            throw ValidationException("Client must have at least one contact method (email or phone)")
        }

        if (client.firstName.isBlank() || client.lastName.isBlank()) {
            throw ValidationException("First name and last name are required")
        }

        if (client.email.isNotBlank() && !isValidEmail(client.email)) {
            throw ValidationException("Invalid email format")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")
        return email.matches(emailRegex)
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
}

@Service
class ClientFacade(
    private val clientService: ClientService,
    private val vehicleRepository: VehicleRepository
) {
    private val logger = LoggerFactory.getLogger(ClientFacade::class.java)

    fun createClient(client: ClientDetails): ClientDetails {
        return clientService.createClient(client)
    }

    fun updateClient(client: ClientDetails): ClientDetails {
        return clientService.updateClient(client)
    }

    fun getClientById(clientId: ClientId): ClientDetails? {
        return clientService.getClientById(clientId)
    }

    fun getAllClients(): List<com.carslab.crm.domain.model.ClientStats> {
        val clients = clientService.getAllClients()
        val clientIds = clients.map { it.id.value }.toSet()
        val vehicles = vehicleRepository.findByClientIds(clientIds)

        return clients.map { client ->
            val stats = clientService.getClientStatistics(client.id)
            val clientVehicles = vehicles[client.id.value] ?: emptyList()

            com.carslab.crm.domain.model.ClientStats(
                client = client,
                vehicles = clientVehicles,
                stats = stats
            )
        }
    }

    fun searchClients(
        name: String? = null,
        email: String? = null,
        phone: String? = null
    ): List<ClientDetails> {
        return clientService.searchClients(name, email, phone)
    }

    fun deleteClient(clientId: ClientId): Boolean {
        return clientService.deleteClient(clientId)
    }

    fun createContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        return clientService.createContactAttempt(contactAttempt)
    }

    fun updateContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        return clientService.updateContactAttempt(contactAttempt)
    }

    fun getContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        return clientService.getContactAttemptsByClientId(clientId)
    }

    fun getContactAttemptById(contactAttemptId: ContactAttemptId): ContactAttempt? {
        return clientService.getContactAttemptById(contactAttemptId)
    }

    fun deleteContactAttempt(contactAttemptId: ContactAttemptId): Boolean {
        return clientService.deleteContactAttempt(contactAttemptId)
    }

    fun getClientStatistics(clientId: ClientId): ClientStats {
        return clientService.getClientStatistics(clientId)
    }
}