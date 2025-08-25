package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.CreateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class VisitClientResolver(
    private val clientQueryService: ClientQueryService,
    private val clientCommandService: ClientCommandService
) {
    private val logger = LoggerFactory.getLogger(VisitClientResolver::class.java)

    fun resolveClient(clientDetails: ClientDetails): ClientResponse {
        clientDetails.ownerId?.let { ownerId ->
            try {
                val existingClient = clientQueryService.getClient(ownerId.toString())
                logger.debug("Found existing client by ID: $ownerId")
                return existingClient.client
            } catch (e: Exception) {
                logger.debug("Client with ID $ownerId not found")
            }
        }

        if (!clientDetails.email.isNullOrBlank() || !clientDetails.phone.isNullOrBlank()) {
            findExistingClientByContact(clientDetails.email, clientDetails.phone)?.let { existingClient ->
                logger.debug("Found existing client by contact: ${existingClient.email}")
                return existingClient
            }
        }

        logger.info("Creating new client: ${clientDetails.name}")
        return createClient(clientDetails)
    }

    private fun findExistingClientByContact(email: String?, phone: String?): ClientResponse? {
        return try {
            clientQueryService.findByPhoneNumberOrEmail(phone, email)
        } catch (e: Exception) {
            logger.warn("Error searching for client by contact", e)
            null
        }
    }

    private fun createClient(clientDetails: ClientDetails): ClientResponse {
        val nameParts = clientDetails.name.split(" ", limit = 2)
        val createRequest = CreateClientRequest(
            firstName = nameParts.getOrNull(0) ?: "",
            lastName = nameParts.getOrNull(1) ?: "",
            email = clientDetails.email ?: "",
            phone = clientDetails.phone ?: "",
            company = clientDetails.companyName,
            taxId = clientDetails.taxId,
            address = clientDetails.address
        )

        return try {
            clientCommandService.createClient(createRequest)
        } catch (e: Exception) {
            logger.warn("Failed to create client, checking if it was created by another thread")
            findExistingClientByContact(clientDetails.email, clientDetails.phone)
                ?: throw IllegalStateException("Could not create or find client: ${clientDetails.name}", e)
        }
    }
}

data class ClientDetails(
    val ownerId: Long? = null,
    val email: String? = null,
    val phone: String? = null,
    val name: String,
    val companyName: String? = null,
    val taxId: String? = null,
    val address: String? = null
)