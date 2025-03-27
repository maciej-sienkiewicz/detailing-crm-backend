package com.carslab.crm.api.controller

import com.carslab.crm.api.mapper.ContactAttemptMapper
import com.carslab.crm.api.model.request.ClientRequest
import com.carslab.crm.api.model.request.ContactAttemptRequest
import com.carslab.crm.api.model.response.ClientExpandedResponse
import com.carslab.crm.domain.ClientFacade
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.ContactAttemptId
import com.carslab.crm.api.model.response.ClientResponse
import com.carslab.crm.api.model.response.ClientStatisticsResponse
import com.carslab.crm.api.model.response.ContactAttemptResponse
import com.carslab.crm.presentation.mapper.ClientMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = ["*"])
class ClientController(
    private val clientFacade: ClientFacade
) {
    private val logger = LoggerFactory.getLogger(ClientController::class.java)


    @PostMapping
    fun createClient(@RequestBody request: ClientRequest): ResponseEntity<ClientExpandedResponse> {
        logger.info("Received request to create new client: ${request.firstName} ${request.lastName}")

        try {
            // Konwertujemy żądanie na model domenowy
            val domainClient = ClientMapper.toDomain(request)

            // Tworzymy klienta za pomocą serwisu
            val createdClient = clientFacade.createClient(domainClient)

            // Konwertujemy wynik na odpowiedź API
            val response = ClientMapper.toExpandedResponse(createdClient)

            logger.info("Successfully created client with ID: ${response.id}")
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error creating client", e)
            throw e
        }
    }

    @GetMapping("/search")
    fun searchClients(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) phone: String?
    ): ResponseEntity<List<ClientResponse>> {
        logger.info("Searching clients with filters: name=$name, email=$email, phone=$phone")

        val clients = clientFacade.searchClients(
            name = name,
            email = email,
            phone = phone
        )

        val response = clients.map { ClientMapper.toResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAllClients(): ResponseEntity<List<ClientExpandedResponse>> {
        logger.info("Getting all clients")

        val clients = clientFacade.getAllClients()
        val response = clients.map { ClientMapper.toExpandedResponse(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getClientById(@PathVariable id: Long): ResponseEntity<ClientResponse> {
        logger.info("Getting client by ID: $id")

        val client = clientFacade.getClientById(ClientId(id))
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ClientMapper.toResponse(client))
    }

    @PutMapping("/{id}")
    fun updateClient(
        @PathVariable id: Long,
        @RequestBody request: ClientRequest
    ): ResponseEntity<ClientResponse> {
        logger.info("Updating client with ID: $id")

        // Sprawdzamy czy klient istnieje
        val existingClient = clientFacade.getClientById(ClientId(id))
            ?: return ResponseEntity.notFound().build()

        try {
            // Upewniamy się, że ID w żądaniu jest zgodne z ID w ścieżce
            val requestWithId = request.apply { this.id = id.toString() }

            // Konwertujemy żądanie na model domenowy, zachowując oryginalne dane audytowe
            val domainClient = ClientMapper.toDomain(requestWithId).copy(
                audit = existingClient.audit.copy(
                    createdAt = existingClient.audit.createdAt
                )
            )

            // Aktualizujemy klienta przy użyciu serwisu
            val updatedClient = clientFacade.updateClient(domainClient)

            // Konwertujemy wynik na odpowiedź API
            val response = ClientMapper.toResponse(updatedClient)

            logger.info("Successfully updated client with ID: $id")
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error updating client with ID: $id", e)
            throw e
        }
    }

    @DeleteMapping("/{id}")
    fun deleteClient(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting client with ID: $id")

        val deleted = clientFacade.deleteClient(ClientId(id))

        return if (deleted) {
            logger.info("Successfully deleted client with ID: $id")
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Client successfully deleted",
                "clientId" to id
            ))
        } else {
            logger.warn("Client with ID: $id not found for deletion")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "success" to false,
                "message" to "Client not found",
                "clientId" to id
            ))
        }
    }

    @PostMapping("/contact-attempts")
    fun createContactAttempt(@RequestBody request: ContactAttemptRequest): ResponseEntity<ContactAttemptResponse> {
        logger.info("Received request to create new contact attempt for client: ${request.clientId}")

        try {
            val domainContactAttempt = ContactAttemptMapper.toDomain(request)

            val createdContactAttempt = clientFacade.createContactAttempt(domainContactAttempt)

            val response = ContactAttemptMapper.toResponse(createdContactAttempt)

            logger.info("Successfully created contact attempt with ID: ${response.id}")
            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            logger.error("Error creating contact attempt", e)
            throw e
        }
    }

    @GetMapping("/{clientId}/contact-attempts")
    fun getContactAttemptsByClientId(
        @PathVariable clientId: String
    ): ResponseEntity<List<ContactAttemptResponse>> {
        logger.info("Getting contact attempts for client: $clientId")

        val contactAttempts = clientFacade.getContactAttemptsByClientId(clientId)
        val response = contactAttempts.map { ContactAttemptMapper.toResponse(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{clientId}/statistics")
    fun getStatistics(@PathVariable clientId: String): ResponseEntity<ClientStatisticsResponse> {

        return ResponseEntity.ok(clientFacade.getClientStatistics(ClientId(clientId.toLong())).let { ClientStatisticsResponse(
            totalVisits = it.visitNo,
            totalRevenue = it.gmv,
            vehicleNo = it.vehiclesNo
        ) })
    }

    @GetMapping("/{clientId}/vehicles")
    fun getVehicles(@PathVariable clientId: String): ResponseEntity<ClientStatisticsResponse> {

        return ResponseEntity.ok(ClientStatisticsResponse(12L, BigDecimal.TEN, 1L))
    }

    @PutMapping("/contact-attempts/{id}")
    fun updateContactAttempt(
        @PathVariable id: String,
        @RequestBody request: ContactAttemptRequest
    ): ResponseEntity<ContactAttemptResponse> {
        logger.info("Updating contact attempt with ID: $id")

        val existingContactAttempt = clientFacade.getContactAttemptById(ContactAttemptId(id))
            ?: return ResponseEntity.notFound().build()

        try {
            val requestWithId = request.apply { this.id = id }

            // Konwertujemy żądanie na model domenowy, zachowując oryginalne dane audytowe
            val domainContactAttempt = ContactAttemptMapper.toDomain(requestWithId).copy(
                audit = existingContactAttempt.audit.copy(
                    createdAt = existingContactAttempt.audit.createdAt
                )
            )

            // Aktualizujemy próbę kontaktu przy użyciu serwisu
            val updatedContactAttempt = clientFacade.updateContactAttempt(domainContactAttempt)

            // Konwertujemy wynik na odpowiedź API
            val response = ContactAttemptMapper.toResponse(updatedContactAttempt)

            logger.info("Successfully updated contact attempt with ID: $id")
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error updating contact attempt with ID: $id", e)
            throw e
        }
    }

    @DeleteMapping("/contact-attempts/{id}")
    fun deleteContactAttempt(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting contact attempt with ID: $id")

        val deleted = clientFacade.deleteContactAttempt(ContactAttemptId(id))

        return if (deleted) {
            logger.info("Successfully deleted contact attempt with ID: $id")
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Contact attempt successfully deleted",
                "contactAttemptId" to id
            ))
        } else {
            logger.warn("Contact attempt with ID: $id not found for deletion")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "success" to false,
                "message" to "Contact attempt not found",
                "contactAttemptId" to id
            ))
        }
    }
}