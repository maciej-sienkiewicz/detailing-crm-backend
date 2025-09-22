package com.carslab.crm.production.modules.clients.presentation

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientExpandedResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.dto.ClientWithStatisticsResponse
import com.carslab.crm.production.modules.clients.application.dto.CreateClientRequest
import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
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
    private val clientCommandService: ClientCommandService,
    private val clientQueryService: ClientQueryService
) : BaseController() {

    @PostMapping
    @HttpMonitored(endpoint = "POST_/api/clients")
    @Operation(summary = "Create a new client", description = "Creates a new client with the provided information")
    fun createClient(@Valid @RequestBody request: CreateClientRequest): ResponseEntity<ClientResponse> {
        logger.info("Received request to create new client: ${request.firstName} ${request.lastName}")

        val response = clientCommandService.createClient(request)
        logger.info("Successfully created client with ID: ${response.id}")

        return created(response)
    }

    @GetMapping("/search")
    @HttpMonitored(endpoint = "GET_/api/clients/search")
    @Operation(summary = "Search clients", description = "Search clients by name, email, or phone number")
    fun searchClients(
        @Parameter(description = "Client name to search for") @RequestParam(required = false) name: String?,
        @Parameter(description = "Client email to search for") @RequestParam(required = false) email: String?,
        @Parameter(description = "Client phone to search for") @RequestParam(required = false) phone: String?
    ): ResponseEntity<List<ClientResponse>> {
        logger.info("Searching clients with filters: name=$name, email=$email, phone=$phone")

        val clients = clientQueryService.searchClients(
            name = name,
            email = email,
            phone = phone,
            company = null,
            minVehicles = null,
            minTotalRevenue = null,
            maxTotalRevenue = null,
            minVisits = null,
            maxVisits = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = clients.content.map { it }
        return ok(response)
    }

    @GetMapping("/paginated")
    @HttpMonitored(endpoint = "GET_/api/clients/paginated")
    @Operation(summary = "Get clients with pagination", description = "Retrieves clients with pagination and filtering")
    fun getClientsPaginated(
        @Parameter(description = "Client name to search for") @RequestParam(required = false) name: String?,
        @Parameter(description = "Client email to search for") @RequestParam(required = false) email: String?,
        @Parameter(description = "Client phone to search for") @RequestParam(required = false) phone: String?,
        @Parameter(description = "Company name to search for") @RequestParam(required = false) company: String?,
        @Parameter(description = "Min vehicles filter") @RequestParam(required = false) min_vehicles: Int?,
        @Parameter(description = "Minimum total revenue") @RequestParam(required = false) min_total_revenue: Double?,
        @Parameter(description = "Maximum total revenue") @RequestParam(required = false) max_total_revenue: Double?,
        @Parameter(description = "Minimum visits") @RequestParam(required = false) min_visits: Int?,
        @Parameter(description = "Maximum visits") @RequestParam(required = false) max_visits: Int?,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") sortBy: String?,
        @Parameter(description = "Sort order") @RequestParam(defaultValue = "asc") sortOrder: String?
    ): ResponseEntity<PaginatedResponse<ClientExpandedResponse>> {
        logger.info("Getting clients with pagination: page=$page, size=$size, name=$name, email=$email, phone=$phone, company=$company")

        val clients = clientQueryService.searchClientsExpanded(
            name = name,
            email = email,
            phone = phone,
            company = company,
            minVehicles = min_vehicles,
            minTotalRevenue = min_total_revenue,
            maxTotalRevenue = max_total_revenue,
            minVisits = min_visits,
            maxVisits = max_visits,
            pageable = PageRequest.of(page, size)
        )

        val response = PaginatedResponse(
            data = clients.content,
            page = clients.number,
            size = clients.size,
            totalItems = clients.totalElements,
            totalPages = clients.totalPages.toLong()
        )

        logger.info("Successfully retrieved ${response.data.size} clients out of ${response.totalItems} total")
        return ok(response)
    }

    @GetMapping
    @HttpMonitored(endpoint = "GET_/api/clients")
    @Operation(summary = "Get all clients", description = "Retrieves all clients with expanded information (legacy endpoint)")
    fun getAllClients(): ResponseEntity<List<ClientResponse>> {
        logger.info("Getting all clients (legacy endpoint)")

        val clients = clientQueryService.searchClients(
            name = null,
            email = null,
            phone = null,
            company = null,
            minVehicles = null,
            minTotalRevenue = null,
            maxTotalRevenue = null,
            minVisits = null,
            maxVisits = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = clients.content
        logger.info("Successfully retrieved ${response.size} clients")
        return ok(response)
    }

    @GetMapping("/{id}")
    @HttpMonitored(endpoint = "GET_/api/clients/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves a client by their ID")
    fun getClientById(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long
    ): ResponseEntity<ClientWithStatisticsResponse> {
        logger.info("Getting client by ID: $id")

        val response = clientQueryService.getClient(id.toString())
        return ok(response)
    }

    @PutMapping("/{id}")
    @HttpMonitored(endpoint = "PUT_/api/clients/{id}")
    @Operation(summary = "Update a client", description = "Updates an existing client with the provided information")
    fun updateClient(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody request: UpdateClientRequest
    ): ResponseEntity<ClientResponse> {
        logger.info("Updating client with ID: $id")

        val response = clientCommandService.updateClient(id.toString(), request)
        logger.info("Successfully updated client with ID: $id")

        return ok(response)
    }

    @DeleteMapping("/{id}")
    @HttpMonitored(endpoint = "DELETE_/api/clients/{id}")
    @Operation(summary = "Delete a client", description = "Deletes a client by their ID")
    fun deleteClient(
        @Parameter(description = "Client ID", required = true) @PathVariable id: Long
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting client with ID: $id")

        clientCommandService.deleteClient(id.toString())
        logger.info("Successfully deleted client with ID: $id")

        return ok(mapOf("message" to "Client successfully deleted", "clientId" to id))
    }

    @GetMapping("/{clientId}/statistics")
    @HttpMonitored(endpoint = "GET_/api/clients/{id}/statistics")
    @Operation(summary = "Get client statistics", description = "Retrieves statistical information about a client")
    fun getStatistics(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<ClientStatisticsResponse> {
        logger.info("Getting statistics for client: $clientId")

        val clientWithStats = clientQueryService.getClient(clientId)

        return ok(
            ClientStatisticsResponse(
                totalVisits = clientWithStats.statistics?.visitCount ?: 0,
                totalRevenue = clientWithStats.statistics?.totalRevenue ?: java.math.BigDecimal.ZERO,
                vehicleNo = clientWithStats.statistics?.vehicleCount ?: 0
            )
        )
    }

    @GetMapping("/{clientId}/vehicles")
    @HttpMonitored(endpoint = "GET_/api/clients/{id}/vehicles")
    @Operation(summary = "Get client vehicles", description = "Retrieves vehicles associated with a client")
    fun getVehicles(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles for client: $clientId")

        return ok(emptyList())
    }
}

data class ClientStatisticsResponse(
    val totalVisits: Long,
    val totalRevenue: java.math.BigDecimal,
    val vehicleNo: Long
)

data class VehicleResponse(
    val id: String,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val displayName: String
)