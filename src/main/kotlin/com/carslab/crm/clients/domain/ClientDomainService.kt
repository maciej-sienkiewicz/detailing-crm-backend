package com.carslab.crm.clients.domain

import com.carslab.crm.api.model.commands.CreateClientCommand
import com.carslab.crm.api.model.commands.UpdateClientCommand
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.clients.domain.model.Client
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.ClientStatistics
import com.carslab.crm.clients.domain.model.ClientWithStatistics
import com.carslab.crm.clients.domain.port.ClientRepository
import com.carslab.crm.clients.domain.port.ClientSearchCriteria
import com.carslab.crm.clients.domain.port.ClientStatisticsRepository
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ClientDomainService(
    private val clientRepository: ClientRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val associationRepository: ClientVehicleAssociationRepository
) {

    fun createClient(command: CreateClientCommand): Client {
        validateClientUniqueness(command.email, command.phone)

        val client = Client(
            id = ClientId.Companion.generate(),
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
            company = command.company,
            taxId = command.taxId,
            notes = command.notes
        )

        val savedClient = clientRepository.save(client)
        initializeClientStatistics(savedClient.id)

        return savedClient
    }

    fun updateClient(id: ClientId, command: UpdateClientCommand): Client {
        val existingClient = clientRepository.findById(id)
            ?: throw DomainException("Client not found: ${id.value}")

        if (command.email != existingClient.email || command.phone != existingClient.phone) {
            validateClientUniqueness(command.email, command.phone, excludeId = id)
        }

        val updatedClient = existingClient.copy(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
            company = command.company,
            taxId = command.taxId,
            notes = command.notes,
            audit = existingClient.audit.updated()
        )

        return clientRepository.save(updatedClient)
    }

    @Transactional(readOnly = true)
    fun getClientById(id: ClientId): Client? = clientRepository.findById(id)

    @Transactional(readOnly = true)
    fun getClientWithStatistics(id: ClientId): ClientWithStatistics? {
        val client = clientRepository.findById(id) ?: return null
        val statistics = clientStatisticsRepository.findByClientId(id)
            ?: ClientStatistics(clientId = id.value)

        return ClientWithStatistics(client, statistics)
    }

    @Transactional(readOnly = true)
    fun searchClients(criteria: ClientSearchCriteria, pageable: Pageable): Page<Client> {
        return clientRepository.searchClients(criteria, pageable)
    }

    fun deleteClient(id: ClientId): Boolean {
        val client = clientRepository.findById(id) ?: return false

        // Remove all vehicle associations
        associationRepository.deleteByClientId(id)

        // Delete statistics
        clientStatisticsRepository.deleteByClientId(id)

        return clientRepository.deleteById(id)
    }

    private fun validateClientUniqueness(email: String, phone: String, excludeId: ClientId? = null) {
        if (email.isNotBlank()) {
            val existingByEmail = clientRepository.findByEmail(email)
            if (existingByEmail != null && existingByEmail.id != excludeId) {
                throw DomainException("Client with email $email already exists")
            }
        }

        if (phone.isNotBlank()) {
            val existingByPhone = clientRepository.findByPhone(phone)
            if (existingByPhone != null && existingByPhone.id != excludeId) {
                throw DomainException("Client with phone $phone already exists")
            }
        }
    }

    private fun initializeClientStatistics(clientId: ClientId) {
        val statistics = ClientStatistics(clientId = clientId.value)
        clientStatisticsRepository.save(statistics)
    }
}