package com.carslab.crm.modules.clients.domain

import com.carslab.crm.modules.clients.api.CreateClientCommand
import com.carslab.crm.modules.clients.api.UpdateClientCommand
import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.modules.clients.domain.model.Client
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.modules.clients.domain.model.ClientWithStatistics
import com.carslab.crm.modules.clients.domain.model.CreateClient
import com.carslab.crm.modules.clients.domain.port.ClientRepositoryDeprecated
import com.carslab.crm.modules.clients.domain.port.ClientSearchCriteria
import com.carslab.crm.modules.clients.domain.port.ClientStatisticsRepositoryDeprecated
import com.carslab.crm.modules.clients.domain.port.ClientVehicleAssociationRepositoryDeprecated
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class ClientDomainServiceDeprecated(
    private val clientRepositoryDeprecated: ClientRepositoryDeprecated,
    private val clientStatisticsRepositoryDeprecated: ClientStatisticsRepositoryDeprecated,
    private val associationRepository: ClientVehicleAssociationRepositoryDeprecated
) {

    fun createClient(command: CreateClientCommand): Client {
        validateClientUniqueness(command.email, command.phone)

        val client = CreateClient(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email ?: "",
            phone = command.phone ?: "",
            address = command.address,
            company = command.company,
            taxId = command.taxId,
            notes = command.notes
        )

        val savedClient = clientRepositoryDeprecated.saveNew(client)
        initializeClientStatistics(savedClient.id)

        return savedClient
    }

    fun updateClient(id: ClientId, command: UpdateClientCommand): Client {
        val existingClient = clientRepositoryDeprecated.findById(id)
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

        return clientRepositoryDeprecated.save(updatedClient)
    }

    fun updateClientStatistics(id: ClientId, gmv: BigDecimal = BigDecimal.ZERO, counter: Long = 0L) {
        clientStatisticsRepositoryDeprecated.updateVisitCount(id, counter)
        clientStatisticsRepositoryDeprecated.updateRevenue(id, gmv)
    }

    @Transactional(readOnly = true)
    fun getClientById(id: ClientId): Client? = clientRepositoryDeprecated.findById(id)

    @Transactional(readOnly = true)
    fun getClientWithStatistics(id: ClientId): ClientWithStatistics? {
        val client = clientRepositoryDeprecated.findById(id) ?: return null
        val statistics = clientStatisticsRepositoryDeprecated.findByClientId(id)
            ?: ClientStatistics(clientId = id.value)

        return ClientWithStatistics(client, statistics)
    }

    @Transactional(readOnly = true)
    fun searchClients(criteria: ClientSearchCriteria, pageable: Pageable): Page<Client> {
        return clientRepositoryDeprecated.searchClients(criteria, pageable)
    }

    fun deleteClient(id: ClientId): Boolean {
        clientRepositoryDeprecated.findById(id) ?: return false

        // Remove all vehicle associations
        associationRepository.deleteByClientId(id)

        // Delete statistics
        clientStatisticsRepositoryDeprecated.deleteByClientId(id)

        return clientRepositoryDeprecated.deleteById(id)
    }

    private fun validateClientUniqueness(email: String?, phone: String?, excludeId: ClientId? = null) {
        if (!email.isNullOrBlank()) {
            val existingByEmail = clientRepositoryDeprecated.findByEmail(email)
            if (existingByEmail != null && existingByEmail.id != excludeId) {
                throw DomainException("Client with email $email already exists")
            }
        }

        if (!phone.isNullOrBlank()) {
            val existingByPhone = clientRepositoryDeprecated.findByPhone(phone)
            if (existingByPhone != null && existingByPhone.id != excludeId) {
                throw DomainException("Client with phone $phone already exists")
            }
        }
    }

    private fun initializeClientStatistics(clientId: ClientId) {
        val statistics = ClientStatistics(clientId = clientId.value)
        clientStatisticsRepositoryDeprecated.save(statistics)
    }
}