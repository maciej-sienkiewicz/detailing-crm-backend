package com.carslab.crm.modules.clients.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.clients.domain.ClientApplicationService
import com.carslab.crm.modules.clients.domain.CreateClientRequest
import com.carslab.crm.modules.clients.domain.UpdateClientRequest
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.carslab.crm.modules.clients.api.mapper.ClientMapper
import com.carslab.crm.modules.clients.api.mapper.VehicleMapper
import com.carslab.crm.modules.clients.api.requests.ClientRequest
import com.carslab.crm.modules.clients.api.responses.ClientExpandedResponse
import com.carslab.crm.modules.clients.api.responses.ClientResponse
import com.carslab.crm.modules.clients.api.responses.ClientStatisticsResponse
import com.carslab.crm.modules.clients.api.responses.VehicleResponse
import com.carslab.crm.modules.clients.domain.ClientDetailResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client management endpoints")
class ClientController(
    private val clientApplicationService: ClientApplicationService
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a new client", description = "Creates a new client with the provided information")
    fun createClient(@Valid @RequestBody request: CreateClientCommand): ResponseEntity<ClientExpandedResponse> {
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

            val appRequest = CreateClientRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phone = request.phone,
                address = request.address,
                company = request.company,
                taxId = request.taxId,
                notes = request.notes
            )

            val createdClient = clientApplicationService.createClient(appRequest)
            val response = ClientExpandedResponse.fromDomain(createdClient)

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

        val clients = clientApplicationService.searchClients(
            name = name,
            email = email,
            phone = phone,
            company = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = clients.content.map { ClientMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get clients with pagination", description = "Retrieves clients with pagination and filtering")
    fun getClientsPaginated(
        @Parameter(description = "Client name to search for") @RequestParam(required = false) name: String?,
        @Parameter(description = "Client email to search for") @RequestParam(required = false) email: String?,
        @Parameter(description = "Client phone to search for") @RequestParam(required = false) phone: String?,
        @Parameter(description = "Company name to search for") @RequestParam(required = false) company: String?,
        @Parameter(description = "Has vehicles filter") @RequestParam(required = false) hasVehicles: Boolean?,
        @Parameter(description = "Minimum total revenue") @RequestParam(required = false) minTotalRevenue: Double?,
        @Parameter(description = "Maximum total revenue") @RequestParam(required = false) maxTotalRevenue: Double?,
        @Parameter(description = "Minimum visits") @RequestParam(required = false) minVisits: Int?,
        @Parameter(description = "Maximum visits") @RequestParam(required = false) maxVisits: Int?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") sortBy: String?,
        @Parameter(description = "Sort order") @RequestParam(defaultValue = "asc") sortOrder: String?
    ): ResponseEntity<PaginatedResponse<ClientExpandedResponse>> {
        logger.info("Getting clients with pagination: page=$page, size=$size, name=$name, email=$email, phone=$phone, company=$company")

        val clients = clientApplicationService.searchClients(
            name = name,
            email = email,
            phone = phone,
            company = company,
            pageable = PageRequest.of(page, size)
        )

        val response = PaginatedResponse(
            data = clients.content.map { ClientExpandedResponse.fromDomain(it) },
            page = clients.number,
            size = clients.size,
            totalItems = clients.totalElements,
            totalPages = clients.totalPages.toLong()
        )

        logger.info("Successfully retrieved ${response.data.size} clients out of ${response.totalItems} total")
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all clients", description = "Retrieves all clients with expanded information (legacy endpoint)")
    fun getAllClients(): ResponseEntity<List<ClientExpandedResponse>> {
        logger.info("Getting all clients (legacy endpoint)")

        val clients = clientApplicationService.searchClients(
            name = null,
            email = null,
            phone = null,
            company = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = clients.content.map { client ->
            ClientExpandedResponse.fromDomain(client)
        }

        logger.info("Successfully retrieved ${response.size} clients")
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves a client by their ID")
    fun getClientById(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long
    ): ResponseEntity<ClientExpandedResponse> {
        logger.info("Getting client by ID: $id")

        val client: ClientDetailResponse = clientApplicationService.getClientById(id)
            ?: throw ResourceNotFoundException("Client", id)

        return ok(ClientExpandedResponse.fromDomain(client))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a client", description = "Updates an existing client with the provided information")
    fun updateClient(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody request: ClientRequest
    ): ResponseEntity<ClientResponse> {
        logger.info("Updating client with ID: $id")

        try {
            ValidationUtils.validateNotBlank(request.firstName, "First name")
            ValidationUtils.validateNotBlank(request.lastName, "Last name")
            ValidationUtils.validateEmail(request.email)
            ValidationUtils.validatePhone(request.phone)
            ValidationUtils.validateAtLeastOneNotBlank(mapOf(
                "Email" to request.email,
                "Phone" to request.phone
            ))

            val appRequest = UpdateClientRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phone = request.phone,
                address = request.address,
                company = request.company,
                taxId = request.taxId,
                notes = request.notes
            )

            val updatedClient = clientApplicationService.updateClient(id, appRequest)
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

        val deleted = clientApplicationService.deleteClient(id)

        return if (deleted) {
            logger.info("Successfully deleted client with ID: $id")
            ok(createSuccessResponse("Client successfully deleted", mapOf("clientId" to id)))
        } else {
            logger.warn("Client with ID: $id not found for deletion")
            throw ResourceNotFoundException("Client", id)
        }
    }

    @GetMapping("/{clientId}/statistics")
    @Operation(summary = "Get client statistics", description = "Retrieves statistical information about a client")
    fun getStatistics(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<ClientStatisticsResponse> {
        logger.info("Getting statistics for client: $clientId")

        val clientWithStats = clientApplicationService.getClientById(clientId.toLong())
            ?: throw ResourceNotFoundException("Client", clientId.toLong())

        return ok(
            ClientStatisticsResponse(
                totalVisits = clientWithStats.statistics.visitCount,
                totalRevenue = clientWithStats.statistics.totalRevenue,
                vehicleNo = clientWithStats.statistics.vehicleCount
            )
        )
    }

    @GetMapping("/{clientId}/vehicles")
    @Operation(summary = "Get client vehicles", description = "Retrieves vehicles associated with a client")
    fun getVehicles(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles for client: $clientId")

        val clientWithDetails = clientApplicationService.getClientById(clientId.toLong())
            ?: throw ResourceNotFoundException("Client", clientId.toLong())

        return ok(clientWithDetails.vehicles.map { VehicleMapper.toResponse(it) })
    }
}