package com.carslab.crm.domain.clients

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.client.CreateClientModel
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.ClientRepository
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.ContactAttemptRepository
import com.carslab.crm.domain.port.VehicleRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.logging.LogOperation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Fasada zarządzająca operacjami na klientach i powiązanych encjach.
 * Dostarcza scentralizowany interfejs dla operacji klienckich w systemie CRM.
 */
@Service
class ClientFacade(
    private val clientRepository: ClientRepository,
    private val contactAttemptRepository: ContactAttemptRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val vehicleRepository: VehicleRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(ClientFacade::class.java)

    @LogOperation(operation = "createClient")
    fun createClient(client: CreateClientModel): ClientDetails {
        logger.info("Tworzenie klienta: {} {}", client.firstName, client.lastName)

        val savedClient = clientRepository.save(client)
            .also { initializeClientStatistics(it.id) }

        return savedClient
    }

    @LogOperation(operation = "updateClient")
    fun updateClient(client: ClientDetails): ClientDetails {
        logger.info("Aktualizacja klienta: {} {}", client.firstName, client.lastName)

        val existingClient = getClientOrThrow(client.id)
        val updatedClient = existingClient.copy(
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            audit = client.audit.copy(updatedAt = LocalDateTime.now())
        )

        return clientRepository.updateOrSave(updatedClient)
    }

    @LogOperation(operation = "getClientById")
    fun getClientById(clientId: ClientId): ClientDetails? {
        logger.debug("Pobieranie klienta po ID: {}", clientId.value)
        return clientRepository.findById(clientId)
    }

    private fun getClientOrThrow(clientId: ClientId): ClientDetails {
        return clientRepository.findById(clientId)
            ?: throw ResourceNotFoundException("Klient", clientId.value)
    }

    @LogOperation(operation = "getVehiclesByClientId")
    fun getVehiclesByClientId(clientId: ClientId): List<Vehicle> {
        logger.debug("Pobieranie pojazdów dla klienta: {}", clientId.value)
        return vehicleRepository.findByClientId(clientId)
    }

    @LogOperation(operation = "getAllClients")
    fun getAllClients(): List<com.carslab.crm.domain.model.ClientStats> {
        logger.debug("Pobieranie wszystkich klientów")

        return clientRepository.findAll().map { client ->
            com.carslab.crm.domain.model.ClientStats(
                client = client,
                vehicles = emptyList(),
                stats = ClientStats(0, 0, 0.toBigDecimal(), 0)
            )
        }
    }

    @LogOperation(operation = "searchClients")
    fun searchClients(
        name: String? = null,
        email: String? = null,
        phone: String? = null
    ): List<ClientDetails> {
        logger.info("Wyszukiwanie klientów z parametrami: name={}, email={}, phone={}", name, email, phone)

        // Optymalizacja wyszukiwania na podstawie dostarczonych filtrów
        return when {
            !name.isNullOrBlank() && email.isNullOrBlank() && phone.isNullOrBlank() -> {
                clientRepository.findByName(name)
            }
            name.isNullOrBlank() && !email.isNullOrBlank() && phone.isNullOrBlank() -> {
                clientRepository.findByEmail(email)
            }
            name.isNullOrBlank() && email.isNullOrBlank() && !phone.isNullOrBlank() -> {
                clientRepository.findByPhone(phone)
            }
            !email.isNullOrBlank() && !phone.isNullOrBlank() -> {
                clientRepository.findClient(email, phone)?.let { listOf(it) } ?: emptyList()
            }
            else -> {
                applyFilters(clientRepository.findAll(), name, email, phone)
            }
        }
    }

    private fun applyFilters(
        clients: List<ClientDetails>,
        name: String?,
        email: String?,
        phone: String?
    ): List<ClientDetails> {
        var filteredResults = clients

        if (!name.isNullOrBlank()) {
            filteredResults = filteredResults.filter {
                it.fullName.contains(name, ignoreCase = true) ||
                        it.firstName.contains(name, ignoreCase = true) ||
                        it.lastName.contains(name, ignoreCase = true)
            }
        }

        if (!email.isNullOrBlank()) {
            filteredResults = filteredResults.filter {
                it.email.contains(email, ignoreCase = true)
            }
        }

        if (!phone.isNullOrBlank()) {
            filteredResults = filteredResults.filter {
                it.phone.replace(" ", "").contains(phone.replace(" ", ""))
            }
        }

        return filteredResults
    }

    @LogOperation(operation = "deleteClient")
    fun deleteClient(clientId: ClientId): Boolean {
        logger.info("Usuwanie klienta: {}", clientId.value)
        return clientRepository.deleteById(clientId)
    }

    @LogOperation(operation = "createContactAttempt")
    fun createContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        logger.info("Tworzenie próby kontaktu dla klienta: {}", contactAttempt.clientId)
        return contactAttemptRepository.save(contactAttempt)
    }

    @LogOperation(operation = "updateContactAttempt")
    fun updateContactAttempt(contactAttempt: ContactAttempt): ContactAttempt {
        logger.info("Aktualizacja próby kontaktu ID: {}, dla klienta: {}",
            contactAttempt.id.value, contactAttempt.clientId)

        getContactAttemptOrThrow(contactAttempt.id)

        val updatedContactAttempt = contactAttempt.copy(
            audit = contactAttempt.audit.copy(updatedAt = LocalDateTime.now())
        )

        return contactAttemptRepository.save(updatedContactAttempt)
    }

    @LogOperation(operation = "getContactAttemptsByClientId")
    fun getContactAttemptsByClientId(clientId: String): List<ContactAttempt> {
        logger.debug("Pobieranie prób kontaktu dla klienta: {}", clientId)
        return contactAttemptRepository.findContactAttemptsByClientId(clientId)
    }

    @LogOperation(operation = "getContactAttemptById")
    fun getContactAttemptById(contactAttemptId: ContactAttemptId): ContactAttempt? {
        logger.debug("Pobieranie próby kontaktu o ID: {}", contactAttemptId.value)
        return contactAttemptRepository.findById(contactAttemptId)
    }

    private fun getContactAttemptOrThrow(contactAttemptId: ContactAttemptId): ContactAttempt {
        return contactAttemptRepository.findById(contactAttemptId)
            ?: throw ResourceNotFoundException("Próba kontaktu", contactAttemptId.value)
    }

    @LogOperation(operation = "deleteContactAttempt")
    fun deleteContactAttempt(contactAttemptId: ContactAttemptId): Boolean {
        logger.info("Usuwanie próby kontaktu: {}", contactAttemptId.value)
        return contactAttemptRepository.deleteById(contactAttemptId)
    }

    /**
     * Zarządzanie statystykami klientów
     */

    @LogOperation(operation = "getClientStatistics")
    fun getClientStatistics(clientId: ClientId): ClientStats {
        logger.debug("Pobieranie statystyk dla klienta: {}", clientId.value)
        return clientStatisticsRepository.findById(clientId)
            ?: ClientStats(clientId.value, 0, "0".toBigDecimal(), 0)
    }

    private fun initializeClientStatistics(clientId: ClientId) {
        logger.debug("Inicjalizacja statystyk dla nowego klienta: {}", clientId.value)
        val stats = ClientStats(
            clientId = clientId.value,
            visitNo = 0,
            gmv = "0".toBigDecimal(),
            vehiclesNo = 0
        )
        clientStatisticsRepository.save(stats)
    }
}