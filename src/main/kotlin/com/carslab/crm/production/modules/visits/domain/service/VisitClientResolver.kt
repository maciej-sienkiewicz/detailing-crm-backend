package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.CreateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
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

    fun resolveClient(request: CreateVisitRequest): ClientResponse {
        request.ownerId?.let { ownerId ->
            try {
                val existingClient = clientQueryService.getClient(ownerId.toString())
                logger.debug("Found existing client by ID: $ownerId")
                return existingClient.client
            } catch (e: Exception) {
                logger.debug("Client with ID $ownerId not found")
            }
        }

        if (!request.email.isNullOrBlank() || !request.phone.isNullOrBlank()) {
            findExistingClientByContact(request.email, request.phone)?.let { existingClient ->
                logger.debug("Found existing client by contact: ${existingClient.email}")
                return existingClient
            }
        }

        logger.info("Creating new client: ${request.ownerName}")
        return createClient(request)
    }

    private fun findExistingClientByContact(email: String?, phone: String?): ClientResponse? {
        return try {
            clientQueryService.searchClients(
                name = null,
                email = email,
                phone = phone,
                company = null,
                hasVehicles = null,
                minTotalRevenue = null,
                maxTotalRevenue = null,
                minVisits = null,
                maxVisits = null,
                pageable = PageRequest.of(0, 1)
            ).content.firstOrNull()
        } catch (e: Exception) {
            logger.warn("Error searching for client by contact", e)
            null
        }
    }

    private fun createClient(request: CreateVisitRequest): ClientResponse {
        val nameParts = request.ownerName.split(" ", limit = 2)
        val createRequest = CreateClientRequest(
            firstName = nameParts.getOrNull(0) ?: "",
            lastName = nameParts.getOrNull(1) ?: "",
            email = request.email ?: "",
            phone = request.phone ?: "",
            company = request.companyName,
            taxId = request.taxId,
            address = request.address
        )

        return try {
            clientCommandService.createClient(createRequest)
        } catch (e: Exception) {
            logger.warn("Failed to create client, checking if it was created by another thread")
            findExistingClientByContact(request.email, request.phone)
                ?: throw IllegalStateException("Could not create or find client: ${request.ownerName}", e)
        }
    }
}