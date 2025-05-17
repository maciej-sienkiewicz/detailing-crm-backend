package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.request.ServiceHistoryRequest
import com.carslab.crm.api.model.response.ServiceHistoryResponse
import com.carslab.crm.api.model.response.VehicleOwnerResponse
import com.carslab.crm.api.model.request.VehicleRequest
import com.carslab.crm.api.mapper.ServiceHistoryMapper
import com.carslab.crm.domain.clients.VehicleFacade
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.api.model.response.VehicleResponse
import com.carslab.crm.api.model.response.VehicleStatisticsResponse
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.util.ValidationUtils
import com.carslab.crm.presentation.mapper.VehicleMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
class VehicleController(
    private val vehicleFacade: VehicleFacade,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a new vehicle", description = "Creates a new vehicle with the provided information")
    fun createVehicle(@Valid @RequestBody request: VehicleRequest): ResponseEntity<VehicleResponse> {
        logger.info("Received request to create new vehicle: ${request.make} ${request.model}, plate: ${request.licensePlate}")

        try {
            // Validate vehicle data
            ValidationUtils.validateNotBlank(request.make, "Make")
            ValidationUtils.validateNotBlank(request.model, "Model")
            ValidationUtils.validateNotBlank(request.licensePlate, "License plate")
            ValidationUtils.validateInRange(request.year, 1900, 2100, "Year")

            if (request.ownerIds.isEmpty()) {
                throw ValidationException("Vehicle must have at least one owner")
            }

            // Convert request to domain model
            val domainVehicle = VehicleMapper.toDomain(request)

            // Create vehicle using service
            val createdVehicle = vehicleFacade.createVehicle(domainVehicle)

            // Convert result to API response
            val response = VehicleMapper.toResponse(createdVehicle)

            logger.info("Successfully created vehicle with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating vehicle", e)
        }
    }

    @GetMapping
    @Operation(summary = "Get all vehicles", description = "Retrieves all vehicles")
    fun getAllVehicles(): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting all vehicles")

        val vehicles = vehicleFacade.getAllVehicles()
        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/{id}/owners")
    @Operation(summary = "Get vehicle owners", description = "Retrieves all owners of a specific vehicle")
    fun getOwners(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<VehicleOwnerResponse>> {
        logger.info("Getting owners for vehicle ID: $id")

        val owners = vehicleFacade.getVehicleOwners(VehicleId(id.toLong()))
        return ok(owners.map { VehicleOwnerResponse(it.id.value, it.fullName) })
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID", description = "Retrieves a vehicle by its ID")
    fun getVehicleById(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleResponse> {
        logger.info("Getting vehicle by ID: $id")

        val vehicle = vehicleFacade.getVehicleById(VehicleId(id.toLong()))
            ?: throw ResourceNotFoundException("Vehicle", id)

        return ok(VehicleMapper.toResponse(vehicle))
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get vehicle statistics", description = "Retrieves statistical information about a vehicle")
    fun getVehicleStatistics(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleStatisticsResponse> {
        logger.info("Getting vehicle statistics: $id")

        val stats = vehicleStatisticsRepository.findById(VehicleId((id.toLong())))
            .let { VehicleStatisticsResponse(it.visitNo, it.gmv) }

        return ok(stats)
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get vehicles by owner", description = "Retrieves all vehicles owned by a specific person")
    fun getVehiclesByOwnerId(
        @Parameter(description = "Owner ID", required = true) @PathVariable ownerId: String
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles by owner ID: $ownerId")

        val vehicles = vehicleFacade.getVehiclesByOwnerId(ownerId)
        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vehicle", description = "Updates an existing vehicle with the provided information")
    fun updateVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: VehicleRequest
    ): ResponseEntity<VehicleResponse> {
        logger.info("Updating vehicle with ID: $id")

        // Verify vehicle exists
        val existingVehicle = findResourceById(
            id,
            vehicleFacade.getVehicleById(VehicleId(id.toLong())),
            "Vehicle"
        )

        try {
            // Validate vehicle data
            ValidationUtils.validateNotBlank(request.make, "Make")
            ValidationUtils.validateNotBlank(request.model, "Model")
            ValidationUtils.validateNotBlank(request.licensePlate, "License plate")
            ValidationUtils.validateInRange(request.year, 1900, 2100, "Year")

            if (request.ownerIds.isEmpty()) {
                throw ValidationException("Vehicle must have at least one owner")
            }

            // Ensure ID in request matches path ID
            val requestWithId = request.apply { this.id = id }

            // Convert request to domain model, preserving original audit data
            val domainVehicle = VehicleMapper.toDomain(requestWithId).copy(
                audit = existingVehicle.audit.copy(
                    createdAt = existingVehicle.audit.createdAt
                )
            )

            // Update vehicle using service
            val updatedVehicle = vehicleFacade.updateVehicle(domainVehicle)

            // Convert result to API response
            val response = VehicleMapper.toResponse(updatedVehicle)

            logger.info("Successfully updated vehicle with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating vehicle with ID: $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a vehicle", description = "Deletes a vehicle by its ID")
    fun deleteVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting vehicle with ID: $id")

        val deleted = vehicleFacade.deleteVehicle(VehicleId(id.toLong()))

        return if (deleted) {
            logger.info("Successfully deleted vehicle with ID: $id")
            ok(createSuccessResponse("Vehicle successfully deleted", mapOf("vehicleId" to id)))
        } else {
            logger.warn("Vehicle with ID: $id not found for deletion")
            throw ResourceNotFoundException("Vehicle", id)
        }
    }

    @GetMapping("/{id}/service-history")
    @Operation(summary = "Get vehicle service history", description = "Retrieves the service history for a specific vehicle")
    fun getVehicleServiceHistory(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<ServiceHistoryResponse>> {
        logger.info("Getting service history for vehicle: $id")

        val serviceHistory = vehicleFacade.getServiceHistoryByVehicleId(VehicleId(id.toLong()))
        val response = serviceHistory.map { ServiceHistoryMapper.toResponse(it) }

        return ok(response)
    }

    @PostMapping("/{id}/service-history")
    @Operation(summary = "Add service history entry", description = "Adds a new service history entry for a vehicle")
    fun addServiceHistoryEntry(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: ServiceHistoryRequest
    ): ResponseEntity<ServiceHistoryResponse> {
        logger.info("Adding service history entry for vehicle: $id")

        try {
            // Verify vehicle exists
            val vehicle = findResourceById(
                id,
                vehicleFacade.getVehicleById(VehicleId(id.toLong())),
                "Vehicle"
            )

            // Validate service history entry
            ValidationUtils.validateNotBlank(request.serviceType, "Service type")
            ValidationUtils.validateNotBlank(request.description, "Description")
            ValidationUtils.validatePositive(request.price, "Price")
            ValidationUtils.validateNotFutureDate(request.date, "Service date")

            // Ensure vehicle ID in request matches path ID
            val requestWithVehicleId = request.apply { this.vehicleId = id }

            // Convert request to domain model
            val domainServiceHistory = ServiceHistoryMapper.toDomain(requestWithVehicleId)

            // Add service history entry
            val createdServiceHistory = vehicleFacade.addServiceHistoryEntry(domainServiceHistory)

            // Convert result to API response
            val response = ServiceHistoryMapper.toResponse(createdServiceHistory)

            logger.info("Successfully added service history entry with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error adding service history entry for vehicle: $id", e)
        }
    }

    @DeleteMapping("/service-history/{id}")
    @Operation(summary = "Delete service history entry", description = "Deletes a service history entry by its ID")
    fun deleteServiceHistoryEntry(
        @Parameter(description = "Service history entry ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting service history entry with ID: $id")

        val deleted = vehicleFacade.deleteServiceHistoryEntry(ServiceHistoryId(id))

        return if (deleted) {
            logger.info("Successfully deleted service history entry with ID: $id")
            ok(createSuccessResponse("Service history entry successfully deleted", mapOf("serviceHistoryId" to id)))
        } else {
            logger.warn("Service history entry with ID: $id not found for deletion")
            throw ResourceNotFoundException("Service history entry", id)
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search vehicles", description = "Search vehicles by license plate, make, or model")
    fun searchVehicles(
        @Parameter(description = "License plate to search for") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Vehicle make to search for") @RequestParam(required = false) make: String?,
        @Parameter(description = "Vehicle model to search for") @RequestParam(required = false) model: String?
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Searching vehicles with filters: licensePlate=$licensePlate, make=$make, model=$model")

        val vehicles = vehicleFacade.searchVehicles(
            licensePlate = licensePlate,
            make = make,
            model = model
        )

        val response = vehicles.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }
}