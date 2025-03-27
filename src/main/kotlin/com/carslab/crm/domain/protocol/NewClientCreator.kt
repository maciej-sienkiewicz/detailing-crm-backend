package com.carslab.crm.domain.protocol

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.port.ClientRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NewClientCreator(
    private val clientRepository: ClientRepository
) {
    private val logger = LoggerFactory.getLogger(NewClientCreator::class.java)

    fun getClient(protocol: CarReceptionProtocol): ClientDetails {
        val client = protocol.client

        // If client has an ID, try to find them
        if (client.id != null) {
            clientRepository.findById(ClientId(client.id))?.let {
                logger.debug("Found existing client with ID: ${client.id}")
                return it
            }
        }

        // Try to find client by contact info
        findClientByContactInfo(client)?.let {
            logger.debug("Found existing client by contact info")
            return it
        }

        // Create new client if none found
        logger.info("Creating new client: ${client.name}")
        return createNewClient(client)
    }

    private fun findClientByContactInfo(client: Client): ClientDetails? {
        return clientRepository.findClient(client)
    }

    private fun createNewClient(client: Client): ClientDetails {
        val nameParts = client.name.split(" ")
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

        return clientRepository.save(
            ClientDetails(
                id = ClientId(),
                firstName = firstName,
                lastName = lastName,
                email = client.email ?: "",
                phone = client.phone ?: "",
                company = client.companyName,
                taxId = client.taxId,
            )
        )
    }
}