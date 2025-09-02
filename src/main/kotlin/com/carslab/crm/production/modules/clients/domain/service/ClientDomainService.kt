package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.command.CreateClientCommand
import com.carslab.crm.production.modules.clients.domain.command.UpdateClientCommand
import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClientDomainService(
    private val clientRepository: ClientRepository,
    private val clientUniquenessValidator: ClientUniquenessValidator,
    private val clientStatisticsInitializer: ClientStatisticsInitializer,
    private val clientAccessValidator: ClientAccessValidator,
    private val clientActivitySender: ClientActivitySender,
    private val clientVisitRecorder: ClientVisitRecorder,
    private val clientVehicleCounter: ClientVehicleCounter
) {
    private val logger = LoggerFactory.getLogger(ClientDomainService::class.java)

    fun createClient(command: CreateClientCommand): Client {
        logger.debug("Creating client: {} {} for company: {}", command.firstName, command.lastName, command.companyId)

        clientUniquenessValidator.validateForCreation(command.email, command.phone, command.companyId)

        val client = Client.from(command)
        val savedClient = clientRepository.save(client)

        clientStatisticsInitializer.initialize(savedClient.id)
        clientActivitySender.onClientCreated(savedClient)

        logger.info("Client created: {} for company: {}", savedClient.id.value, command.companyId)
        return savedClient
    }

    fun updateClient(clientId: ClientId, command: UpdateClientCommand, companyId: Long): Client {
        logger.debug("Updating client: {} for company: {}", clientId.value, companyId)

        val existingClient = clientAccessValidator.getClientForCompany(clientId, companyId)

        if (command.email != existingClient.email || command.phone != existingClient.phone) {
            clientUniquenessValidator.validateForUpdate(command.email, command.phone, companyId, clientId)
        }
        
        if(clientDataChanged(existingClient, command).not()) {
            logger.info("No changes detected for client: {}. Update operation skipped.", clientId.value)
            return existingClient
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
        clientActivitySender.onClientUpdated(existingClient, saved)

        logger.info("Client updated: {} for company: {}", clientId.value, companyId)
        return saved
    }

    private fun clientDataChanged(
        existingClient: Client,
        command: UpdateClientCommand
    ): Boolean {
        return existingClient.firstName != command.firstName.trim() ||
               existingClient.lastName != command.lastName.trim() ||
               existingClient.email != command.email.trim() ||
               existingClient.phone != command.phone.trim() ||
               (existingClient.address ?: "") != (command.address?.trim() ?: "") ||
               (existingClient.company ?: "") != (command.company?.trim() ?: "") ||
               (existingClient.taxId ?: "") != (command.taxId?.trim() ?: "") ||
               (existingClient.notes ?: "") != (command.notes?.trim() ?: "")
    }

    fun getClientForCompany(clientId: ClientId, companyId: Long): Client {
        return clientAccessValidator.getClientForCompany(clientId, companyId)
    }

    fun deleteClient(clientId: ClientId, companyId: Long): Boolean {
        logger.debug("Deleting client: {} for company: {}", clientId.value, companyId)

        val existingClient = clientAccessValidator.getClientForCompany(clientId, companyId)
        val deleted = clientRepository.deleteById(clientId)

        if (deleted) {
            clientActivitySender.onClientDeleted(existingClient)
            logger.info("Client deleted: {} for company: {}", clientId.value, companyId)
        }

        return deleted
    }

    fun findByIds(clientIds: List<ClientId>, companyId: Long): List<Client> {
        return clientRepository.findByIds(clientIds, companyId)
    }

    fun recordVisit(clientId: ClientId) {
        clientVisitRecorder.recordVisit(clientId)
    }

    fun incrementVehicleCount(clientId: ClientId) {
        clientVehicleCounter.incrementVehicleCount(clientId)
    }
}