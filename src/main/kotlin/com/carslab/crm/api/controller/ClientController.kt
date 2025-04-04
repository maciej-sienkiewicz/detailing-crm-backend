package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
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
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.carslab.crm.presentation.mapper.ClientMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = ["*"])
@Tag(name = "Clients", description = "Client management endpoints")
class ClientController(
    private val clientFacade: ClientFacade
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a new client", description = "Creates a new client with the provided information")
    fun createClient(@Valid @RequestBody request: ClientRequest): ResponseEntity<ClientExpandedResponse> {
        logger.info("Received request to create new client: ${request.firstName} ${request.lastName}")

        try {
            ValidationUtils.validateNotBlank(request.firstName, "First name")
            ValidationUtils.validateNotBlank(request.lastName, "Last name")
            ValidationUtils.validateEmail(request.email)
            ValidationUtils.validatePhone(request.phone)
            ValidationUtils.validateAtLeastOneNotBlank(mapOf(
                "Email" to request.email,
                "Phone" to request.phone
            ))

            val domainClient = ClientMapper.toDomain(request)
            val createdClient = clientFacade.createClient(domainClient)
            val response = ClientMapper.toExpandedResponse(createdClient)

            logger.info("Successfully created client with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating client", e)
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search clients", description = "Search clients by name, email, or phone number")
    fun searchClients(
        @Parameter(description = "Client name to search for") @RequestParam(required = false) name: String?,
        @Parameter(description = "Client email to search for") @RequestParam(required = false) email: String?,
        @Parameter(description = "Client phone to search for") @RequestParam(required = false) phone: String?
    ): ResponseEntity<List<ClientResponse>> {
        logger.info("Searching clients with filters: name=$name, email=$email, phone=$phone")

        val clients = clientFacade.searchClients(
            name = name,
            email = email,
            phone = phone
        )

        val response = clients.map { ClientMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all clients", description = "Retrieves all clients with expanded information")
    fun getAllClients(): ResponseEntity<List<ClientExpandedResponse>> {
        logger.info("Getting all clients")

        val clientStats = clientFacade.getAllClients()
        val response = clientStats.map { stats ->
            ClientMapper.toExpandedResponse(stats)  // To powinno być ok, bo teraz mamy ClientStats
        }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves a client by their ID")
    fun getClientById(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long
    ): ResponseEntity<ClientResponse> {
        logger.info("Getting client by ID: $id")

        val client = clientFacade.getClientById(ClientId(id))
            ?: throw ResourceNotFoundException("Client", id)

        return ok(ClientMapper.toResponse(client))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a client", description = "Updates an existing client with the provided information")
    fun updateClient(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody request: ClientRequest
    ): ResponseEntity<ClientResponse> {
        logger.info("Updating client with ID: $id")

        val existingClient = findResourceById(
            id,
            clientFacade.getClientById(ClientId(id)),
            "Client"
        )

        try {
            ValidationUtils.validateNotBlank(request.firstName, "First name")
            ValidationUtils.validateNotBlank(request.lastName, "Last name")
            ValidationUtils.validateEmail(request.email)
            ValidationUtils.validatePhone(request.phone)
            ValidationUtils.validateAtLeastOneNotBlank(mapOf(
                "Email" to request.email,
                "Phone" to request.phone
            ))

            val requestWithId = request.apply { this.id = id.toString() }
            val domainClient = ClientMapper.toDomain(requestWithId).copy(
                audit = existingClient.audit.copy(
                    createdAt = existingClient.audit.createdAt
                )
            )

            val updatedClient = clientFacade.updateClient(domainClient)
            val response = ClientMapper.toResponse(updatedClient)

            logger.info("Successfully updated client with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating client with ID: $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a client", description = "Deletes a client by their ID")
    fun deleteClient(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting client with ID: $id")

        val deleted = clientFacade.deleteClient(ClientId(id))

        return if (deleted) {
            logger.info("Successfully deleted client with ID: $id")
            ok(createSuccessResponse("Client successfully deleted", mapOf("clientId" to id)))
        } else {
            logger.warn("Client with ID: $id not found for deletion")
            throw ResourceNotFoundException("Client", id)
        }
    }

    @PostMapping("/contact-attempts")
    @Operation(summary = "Create a contact attempt", description = "Records a new contact attempt for a client")
    fun createContactAttempt(@Valid @RequestBody request: ContactAttemptRequest): ResponseEntity<ContactAttemptResponse> {
        logger.info("Received request to create new contact attempt for client: ${request.clientId}")

        try {
            ValidationUtils.validateNotBlank(request.clientId, "Client ID")
            ValidationUtils.validateNotBlank(request.type, "Contact type")
            ValidationUtils.validateNotBlank(request.description, "Description")
            ValidationUtils.validateNotBlank(request.result, "Result")

            val domainContactAttempt = ContactAttemptMapper.toDomain(request)
            val createdContactAttempt = clientFacade.createContactAttempt(domainContactAttempt)
            val response = ContactAttemptMapper.toResponse(createdContactAttempt)

            logger.info("Successfully created contact attempt with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating contact attempt", e)
        }
    }

    @GetMapping("/{clientId}/contact-attempts")
    @Operation(summary = "Get client contact attempts", description = "Retrieves all contact attempts for a specific client")
    fun getContactAttemptsByClientId(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<List<ContactAttemptResponse>> {
        logger.info("Getting contact attempts for client: $clientId")

        val contactAttempts = clientFacade.getContactAttemptsByClientId(clientId)
        val response = contactAttempts.map { ContactAttemptMapper.toResponse(it) }

        return ok(response)
    }

    @GetMapping("/{clientId}/statistics")
    @Operation(summary = "Get client statistics", description = "Retrieves statistical information about a client")
    fun getStatistics(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<ClientStatisticsResponse> {
        logger.info("Getting statistics for client: $clientId")

        return ok(clientFacade.getClientStatistics(ClientId(clientId.toLong())).let {
            ClientStatisticsResponse(
                totalVisits = it.visitNo,
                totalRevenue = it.gmv,
                vehicleNo = it.vehiclesNo
            )
        })
    }

    @GetMapping("/{clientId}/vehicles")
    @Operation(summary = "Get client vehicles", description = "Retrieves vehicles associated with a client")
    fun getVehicles(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<ClientStatisticsResponse> {
        logger.info("Getting vehicles for client: $clientId")

        return ok(ClientStatisticsResponse(12L, BigDecimal.TEN, 1L))
    }

    @PutMapping("/contact-attempts/{id}")
    @Operation(summary = "Update a contact attempt", description = "Updates an existing contact attempt")
    fun updateContactAttempt(
        @Parameter(description = "Contact attempt ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: ContactAttemptRequest
    ): ResponseEntity<ContactAttemptResponse> {
        logger.info("Updating contact attempt with ID: $id")

        val existingContactAttempt = findResourceById(
            id,
            clientFacade.getContactAttemptById(ContactAttemptId(id)),
            "Contact attempt"
        )

        try {
            ValidationUtils.validateNotBlank(request.clientId, "Client ID")
            ValidationUtils.validateNotBlank(request.type, "Contact type")
            ValidationUtils.validateNotBlank(request.description, "Description")
            ValidationUtils.validateNotBlank(request.result, "Result")

            val requestWithId = request.apply { this.id = id }
            val domainContactAttempt = ContactAttemptMapper.toDomain(requestWithId).copy(
                audit = existingContactAttempt.audit.copy(
                    createdAt = existingContactAttempt.audit.createdAt
                )
            )

            val updatedContactAttempt = clientFacade.updateContactAttempt(domainContactAttempt)
            val response = ContactAttemptMapper.toResponse(updatedContactAttempt)

            logger.info("Successfully updated contact attempt with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating contact attempt with ID: $id", e)
        }
    }

    @DeleteMapping("/contact-attempts/{id}")
    @Operation(summary = "Delete a contact attempt", description = "Deletes a contact attempt by its ID")
    fun deleteContactAttempt(
        @Parameter(description = "Contact attempt ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting contact attempt with ID: $id")

        val deleted = clientFacade.deleteContactAttempt(ContactAttemptId(id))

        return if (deleted) {
            logger.info("Successfully deleted contact attempt with ID: $id")
            ok(createSuccessResponse("Contact attempt successfully deleted", mapOf("contactAttemptId" to id)))
        } else {
            logger.warn("Contact attempt with ID: $id not found for deletion")
            throw ResourceNotFoundException("Contact attempt", id)
        }
    }
}