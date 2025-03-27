package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.ContactAttemptRepository
import com.carslab.crm.domain.port.VehicleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClientFacade(
    private val clientRepository: ClientRepository,
    private val contactAttemptRepository: ContactAttemptRepository,
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
) {

    private val logger = LoggerFactory.getLogger(ClientFacade::class.java)

    fun createClient(client: ClientDetails): ClientDetails {
        logger.info("Creating new client: ${client.fullName}")

        validateClient(client)

        // Zapisujemy klienta w repozytorium
        val savedClient = clientRepository.save(client)
        logger.info("Created client with ID: ${savedClient.id.value}")
        return savedClient
    }

    fun updateClient(client: ClientDetails): ClientDetails {
        logger.info("Updating client with ID: ${client.id.value}")

        // Sprawdzamy czy klient istnieje
        val existingClient = clientRepository.findById(client.id)
            ?: throw IllegalArgumentException("Client with ID ${client.id.value} not found")

        // Walidacja
        validateClient(client)

        // Aktualizujemy informacje audytowe
        val updatedClient = client.copy(
            audit = client.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        // Zapisujemy zaktualizowanego klienta
        val savedClient = clientRepository.save(updatedClient)
        logger.info("Updated client with ID: ${savedClient.id.value}")
        return savedClient
    }

    fun getClientById(clientId: ClientId): ClientDetails? {
        logger.debug("Getting client by ID: ${clientId.value}")
        return clientRepository.findById(clientId)
    }

    fun getAllClients(): List<ClientStats> {
        logger.debug("Getting all clients")
        val clients = clientRepository.findAll()
        val vehicles = vehicleRepository.findByClientIds(clients.map { it.id.value }.toSet())
        val stats = clients.map {
            clientStatisticsRepository.findById(it.id)
        }.associateBy { it?.clientId }

        return clients.map {
            ClientStats(it, vehicles.get(it.id.value) ?: emptyList(), stats[it.id.value])
        }
    }

    fun searchClients(
        name: String? = null,
        email: String? = null,
        phone: String? = null
    ): List<ClientDetails> {
        logger.debug("Searching clients with filters: name=$name, email=$email, phone=$phone")

        // Pobieramy wszystkich klientów, a następnie filtrujemy ich według podanych kryteriów
        var result = clientRepository.findAll()

        // Filtrowanie według nazwy
        if (!name.isNullOrBlank()) {
            result = result.filter {
                it.fullName.contains(name, ignoreCase = true) ||
                        it.firstName.contains(name, ignoreCase = true) ||
                        it.lastName.contains(name, ignoreCase = true)
            }
        }

        // Filtrowanie według emaila
        if (!email.isNullOrBlank()) {
            result = result.filter {
                it.email.contains(email, ignoreCase = true)
            }
        }

        // Filtrowanie według telefonu
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

        // Sprawdzamy czy próba kontaktu istnieje
        val existingContactAttempt = contactAttemptRepository.findById(contactAttempt.id)
            ?: throw IllegalArgumentException("Contact attempt with ID ${contactAttempt.id.value} not found")

        validateContactAttempt(contactAttempt)

        // Aktualizujemy informacje audytowe
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

    fun getClientStatistics(clientId: ClientId): com.carslab.crm.domain.model.stats.ClientStats =
        clientStatisticsRepository.findById(clientId) ?: com.carslab.crm.domain.model.stats.ClientStats(clientId.value, 0, "0".toBigDecimal(), 0)

    private fun validateContactAttempt(contactAttempt: ContactAttempt) {
        // Sprawdzamy, czy opis nie jest pusty
        if (contactAttempt.description.isBlank()) {
            throw IllegalArgumentException("Contact attempt description cannot be empty")
        }

        // Sprawdzamy, czy data nie jest z przyszłości
        if (contactAttempt.date > LocalDateTime.now()) {
            throw IllegalArgumentException("Contact attempt date cannot be in the future")
        }

        // Sprawdzamy, czy ID klienta nie jest puste
        if (contactAttempt.clientId.isBlank()) {
            throw IllegalArgumentException("Client ID cannot be empty")
        }
    }

    // Prywatna metoda pomocnicza do walidacji klienta
    private fun validateClient(client: ClientDetails) {
        // Sprawdzamy, czy klient ma prawidłowe dane kontaktowe
        if (!client.hasValidContactInfo()) {
            throw IllegalArgumentException("Client must have at least one contact method (email or phone)")
        }

        // Sprawdzamy, czy imię i nazwisko nie są puste
        if (client.firstName.isBlank() || client.lastName.isBlank()) {
            throw IllegalArgumentException("First name and last name are required")
        }

        // Opcjonalna walidacja formatu emaila
        if (client.email.isNotBlank() && !isValidEmail(client.email)) {
            throw IllegalArgumentException("Invalid email format")
        }
    }

    // Prosta walidacja formatu emaila
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")
        return email.matches(emailRegex)
    }
}