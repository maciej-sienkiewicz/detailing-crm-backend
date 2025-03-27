package com.carslab.crm.domain.protocol

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.Client
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.port.ClientRepository
import org.springframework.stereotype.Service

@Service
class NewClientCreator(
    private val clientRepository: ClientRepository
) {
    fun getClient(protocol: CarReceptionProtocol): ClientDetails {
        val client = protocol.client

        return clientRepository.findClient(client) ?: createNewClient(client)
    }

    private fun createNewClient(client: Client): ClientDetails = clientRepository.save(
        ClientDetails(
            id = ClientId(),
            firstName = client.name.split(" ")[0],
            lastName = client.name.split(" ")[1],
            email = client.email ?: "",
            phone = client.phone ?: "",
            company = client.companyName,
            taxId = client.taxId,
        )
    )
}