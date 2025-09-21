package com.carslab.crm.production.modules.associations.presentation

import com.carslab.crm.production.modules.associations.application.dto.AssociationResponse
import com.carslab.crm.production.modules.associations.application.dto.CreateAssociationRequest
import com.carslab.crm.production.modules.associations.application.service.AssociationCommandService
import com.carslab.crm.production.modules.associations.application.service.AssociationQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/associations")
@Tag(name = "Associations", description = "Client-Vehicle association management endpoints")
class AssociationController(
    private val associationCommandService: AssociationCommandService,
    private val associationQueryService: AssociationQueryService
) : BaseController() {

    @PostMapping
    @HttpMonitored(endpoint = "POST_/api/associations")
    @Operation(summary = "Create association", description = "Creates a new association between client and vehicle")
    fun createAssociation(@Valid @RequestBody request: CreateAssociationRequest): ResponseEntity<AssociationResponse> {
        logger.info("Received request to create association between client: {} and vehicle: {}",
            request.clientId, request.vehicleId)

        val response = associationCommandService.createAssociation(request)
        logger.info("Successfully created association")

        return created(response)
    }

    @DeleteMapping("/client/{clientId}/vehicle/{vehicleId}")
    @HttpMonitored(endpoint = "DELETE_/api/associations/client/{clientId}/vehicle/{vehicleId}")
    @Operation(summary = "End association", description = "Ends the association between client and vehicle")
    fun endAssociation(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String,
        @Parameter(description = "Vehicle ID", required = true) @PathVariable vehicleId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Ending association between client: {} and vehicle: {}", clientId, vehicleId)

        associationCommandService.endAssociation(clientId, vehicleId)
        logger.info("Successfully ended association")

        return ok(mapOf("message" to "Association ended successfully"))
    }

    @PutMapping("/client/{clientId}/vehicle/{vehicleId}/primary")
    @HttpMonitored(endpoint = "PUT_/api/associations/client/{clientId}/vehicle/{vehicleId}/primary")
    @Operation(summary = "Make primary owner", description = "Makes the client the primary owner of the vehicle")
    fun makePrimaryOwner(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String,
        @Parameter(description = "Vehicle ID", required = true) @PathVariable vehicleId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Making client: {} primary owner of vehicle: {}", clientId, vehicleId)

        associationCommandService.makePrimaryOwner(clientId, vehicleId)
        logger.info("Successfully updated primary owner")

        return ok(mapOf("message" to "Primary owner updated successfully"))
    }

    @GetMapping("/client/{clientId}/vehicles")
    @HttpMonitored(endpoint = "GET_/api/associations/client/{clientId}/vehicles")
    @Operation(summary = "Get client vehicles", description = "Retrieves all vehicles associated with a client")
    fun getClientVehicles(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: String
    ): ResponseEntity<List<String>> {
        logger.info("Getting vehicles for client: {}", clientId)

        val vehicleIds = associationQueryService.getClientVehicles(clientId)
        val response = vehicleIds.map { it.value.toString() }

        return ok(response)
    }

    @GetMapping("/vehicle/{vehicleId}/clients")
    @HttpMonitored(endpoint = "GET_/api/associations/vehicle/{vehicleId}/clients")
    @Operation(summary = "Get vehicle clients", description = "Retrieves all clients associated with a vehicle")
    fun getVehicleClients(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable vehicleId: String
    ): ResponseEntity<List<String>> {
        logger.info("Getting clients for vehicle: {}", vehicleId)

        val clientIds = associationQueryService.getVehicleClients(vehicleId)
        val response = clientIds.map { it.value.toString() }

        return ok(response)
    }
}