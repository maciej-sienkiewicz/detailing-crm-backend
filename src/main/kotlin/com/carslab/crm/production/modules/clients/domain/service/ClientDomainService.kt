package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.command.CreateClientCommand
import com.carslab.crm.production.modules.clients.domain.command.UpdateClientCommand
import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClientDomainService(
    private val clientRepository: ClientRepository,
    private val clientActivitySender: ClientActivitySender,
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(ClientDomainService::class.java)

    fun createClient(command: CreateClientCommand): Client {
        logger.debug("Creating client: {} {} for company: {}", command.firstName, command.lastName, command.companyId)

        validateClientUniqueness(command.email, command.phone, command.companyId)

        val client = Client(
            id = ClientId(0),
            companyId = command.companyId,
            firstName = command.firstName.trim(),
            lastName = command.lastName.trim(),
            email = command.email.trim(),
            phone = command.phone.trim(),
            address = command.address?.trim(),
            company = command.company?.trim(),
            taxId = command.taxId?.trim(),
            notes = command.notes?.trim(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        val savedClient = clientRepository.save(client)
        initializeClientStatistics(savedClient.id)

        logger.info("Client created: {} for company: {}", savedClient.id.value, command.companyId)
        return savedClient
            .also { clientActivitySender.onClientCreated(it) }
    }

    fun updateClient(clientId: ClientId, command: UpdateClientCommand, companyId: Long): Client {
        logger.debug("Updating client: {} for company: {}", clientId.value, companyId)

        val existingClient = getClientForCompany(clientId, companyId)

        if (command.email != existingClient.email || command.phone != existingClient.phone) {
            validateClientUniqueness(command.email, command.phone, companyId, excludeClientId = clientId)
        }

        val updatedClient = existingClient.update(
            firstName = command.firstName.trim(),
            lastName = command.lastName.trim(),
            email = command.email.trim(),
            phone = command.phone.trim(),
            address = command.address?.trim(),
            company = command.company?.trim(),
            taxId = command.taxId?.trim(),
            notes = command.notes?.trim()
        )

        val saved = clientRepository.save(updatedClient)
        logger.info("Client updated: {} for company: {}", clientId.value, companyId)
        return saved
            .also { clientActivitySender.onClientUpdated(existingClient, it) }
    }

    fun getClientForCompany(clientId: ClientId, companyId: Long): Client {
        val client = clientRepository.findById(clientId)
            ?: throw BusinessException("Client not found: ${clientId.value}")

        if (!client.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to client: ${clientId.value}")
        }

        return client
    }

    fun deleteClient(clientId: ClientId, companyId: Long): Boolean {
        logger.debug("Deleting client: {} for company: {}", clientId.value, companyId)

        getClientForCompany(clientId, companyId)

        val existingClient = getClientForCompany(clientId, companyId)
        clientStatisticsRepository.deleteByClientId(clientId)
        val deleted = clientRepository.deleteById(clientId)

        if (deleted) {
            logger.info("Client deleted: {} for company: {}", clientId.value, companyId)
        }

        return deleted
            .also { clientActivitySender.onClientDeleted(existingClient) }
    }

    private fun validateClientUniqueness(
        email: String,
        phone: String,
        companyId: Long,
        excludeClientId: ClientId? = null
    ) {
        val existingByEmail = clientRepository.findByEmail(email, companyId)
        if (existingByEmail != null && existingByEmail.id != excludeClientId) {
            throw BusinessException("Client with email $email already exists")
        }

        val existingByPhone = clientRepository.findByPhone(phone, companyId)
        if (existingByPhone != null && existingByPhone.id != excludeClientId) {
            throw BusinessException("Client with phone $phone already exists")
        }
    }

    private fun initializeClientStatistics(clientId: ClientId) {
        val statistics = ClientStatistics(clientId = clientId)
        clientStatisticsRepository.save(statistics)
    }

    fun findByIds(clientIds: List<ClientId>, companyId: Long) =
        clientRepository.findByIds(clientIds, companyId)
    
    fun recordVisit(clientId: ClientId) {
        logger.debug("Recording visit for client: {}", clientId.value)

        clientStatisticsRepository.incrementVisitCount(clientId)
        clientStatisticsRepository.setLastVisitDate(clientId, LocalDateTime.now())

        logger.info("Visit recorded for client: {}", clientId.value)
    }
}