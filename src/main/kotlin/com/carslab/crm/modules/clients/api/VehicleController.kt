package com.carslab.crm.modules.clients.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.clients.api.mapper.VehicleMapper
import com.carslab.crm.modules.clients.api.requests.ServiceHistoryRequest
import com.carslab.crm.modules.clients.api.requests.VehicleRequest
import com.carslab.crm.modules.clients.api.responses.*
import com.carslab.crm.modules.clients.api.responses.VehicleResponse
import com.carslab.crm.modules.clients.api.responses.VehicleStatisticsResponse
import com.carslab.crm.modules.clients.domain.*
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.util.ValidationUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management endpoints")
class VehicleController(
    private val vehicleApplicationService: VehicleApplicationService,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleTableService: VehicleTableService,
    private val vehicleCompanyStatisticsService: VehicleCompanyStatisticsService
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

            if (request.ownerIds.isEmpty()) {
                throw ValidationException("Vehicle must have at least one owner")
            }

            val appRequest = CreateVehicleRequest(
                make = request.make,
                model = request.model,
                year = request.year,
                licensePlate = request.licensePlate,
                color = request.color,
                vin = request.vin,
                mileage = request.mileage,
                ownerIds = request.ownerIds
            )

            val createdVehicle = vehicleApplicationService.createVehicle(appRequest)
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

        val vehicles = vehicleApplicationService.searchVehicles(
            make = null,
            model = null,
            licensePlate = null,
            vin = null,
            year = null,
            pageable = PageRequest.of(0, 1000)
        )
        val response = vehicles.content.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/table")
    @Operation(
        summary = "Get vehicles for table view",
        description = "Retrieves vehicles with statistics for table display including owners, visit count, last visit, and total revenue"
    )
    fun getVehiclesForTable(
        @PageableDefault(size = 20, sort = ["lastVisitDate"], direction = Sort.Direction.DESC)
        pageable: Pageable,

        @Parameter(description = "Filter by vehicle make")
        @RequestParam(required = false) make: String?,

        @Parameter(description = "Filter by vehicle model")
        @RequestParam(required = false) model: String?,

        @Parameter(description = "Filter by license plate")
        @RequestParam(required = false) licensePlate: String?,

        @Parameter(description = "Filter by owner name")
        @RequestParam(required = false) ownerName: String?,

        @Parameter(description = "Minimum number of visits")
        @RequestParam(required = false) minVisits: Int?,

        @Parameter(description = "Maximum number of visits")
        @RequestParam(required = false) maxVisits: Int?
    ): ResponseEntity<Page<VehicleTableResponse>> {
        logger.info("Getting vehicles for table view with filters: make=$make, model=$model, licensePlate=$licensePlate, ownerName=$ownerName")

        try {
            val vehicleTablePage = vehicleTableService.getVehiclesForTable(
                pageable = pageable,
                make = make,
                model = model,
                licensePlate = licensePlate,
                ownerName = ownerName,
                minVisits = minVisits,
                maxVisits = maxVisits
            )

            logger.info("Successfully retrieved ${vehicleTablePage.numberOfElements} vehicles for table view")
            return ok(vehicleTablePage)
        } catch (e: Exception) {
            return logAndRethrow("Error retrieving vehicles for table view", e)
        }
    }

    @GetMapping("/company-statistics")
    @Operation(
        summary = "Get company vehicle statistics",
        description = "Retrieves overall vehicle statistics for the company including total vehicles, premium vehicles, and visit revenue median"
    )
    fun getCompanyVehicleStatistics(): ResponseEntity<VehicleCompanyStatisticsResponse> {
        logger.info("Getting company vehicle statistics")

        try {
            val statistics = vehicleCompanyStatisticsService.getCompanyStatistics()

            logger.info("Successfully retrieved company vehicle statistics: ${statistics.totalVehicles} total vehicles, ${statistics.premiumVehicles} premium vehicles")
            return ok(statistics)
        } catch (e: Exception) {
            return logAndRethrow("Error retrieving company vehicle statistics", e)
        }
    }

    @GetMapping("/{id}/owners")
    @Operation(summary = "Get vehicle owners", description = "Retrieves all owners of a specific vehicle")
    fun getOwners(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<List<VehicleOwnerResponse>> {
        logger.info("Getting owners for vehicle ID: $id")

        val vehicleDetail = vehicleApplicationService.getVehicleById(id.toLong())
            ?: throw ResourceNotFoundException("Vehicle", id)

        val owners = vehicleDetail.owners.map {
            VehicleOwnerResponse(it.id, it.fullName)
        }
        return ok(owners)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID", description = "Retrieves a vehicle by its ID")
    fun getVehicleById(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleResponse> {
        logger.info("Getting vehicle by ID: $id")

        val vehicle = vehicleApplicationService.getVehicleById(id.toLong())
            ?: throw ResourceNotFoundException("Vehicle", id)

        return ok(VehicleMapper.toResponse(vehicle))
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get vehicle statistics", description = "Retrieves statistical information about a vehicle")
    fun getVehicleStatistics(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String
    ): ResponseEntity<VehicleStatisticsResponse> {
        logger.info("Getting vehicle statistics: $id")

        val vehicleDetail = vehicleApplicationService.getVehicleById(id.toLong())
            ?: throw ResourceNotFoundException("Vehicle", id)

        val stats = VehicleStatisticsResponse(
            visitNo = vehicleDetail.statistics.visitCount,
            servicesNo = vehicleDetail.serviceInfo.totalServices,
            totalRevenue = vehicleDetail.statistics.totalRevenue
        )

        return ok(stats)
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get vehicles by owner", description = "Retrieves all vehicles owned by a specific person")
    fun getVehiclesByOwnerId(
        @Parameter(description = "Owner ID", required = true) @PathVariable ownerId: String
    ): ResponseEntity<List<VehicleResponse>> {
        logger.info("Getting vehicles by owner ID: $ownerId")

        val clientDetail = clientApplicationService.getClientById(ownerId.toLong())
            ?: throw ResourceNotFoundException("Client", ownerId.toLong())

        val response = clientDetail.vehicles.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vehicle", description = "Updates an existing vehicle with the provided information")
    fun updateVehicle(
        @Parameter(description = "Vehicle ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: VehicleRequest
    ): ResponseEntity<VehicleResponse> {
        logger.info("Updating vehicle with ID: $id")

        try {
            // Validate vehicle data
            ValidationUtils.validateNotBlank(request.make, "Make")
            ValidationUtils.validateNotBlank(request.model, "Model")
            ValidationUtils.validateNotBlank(request.licensePlate, "License plate")

            if (request.ownerIds.isEmpty()) {
                throw ValidationException("Vehicle must have at least one owner")
            }

            val appRequest = UpdateVehicleRequest(
                make = request.make,
                model = request.model,
                year = request.year,
                licensePlate = request.licensePlate,
                color = request.color,
                vin = request.vin,
                mileage = request.mileage
            )

            val updatedVehicle = vehicleApplicationService.updateVehicle(id.toLong(), appRequest)
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

        val deleted = vehicleApplicationService.deleteVehicle(id.toLong())

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

        // For now, return empty list - this would need to be implemented with a proper service history service
        val serviceHistory = emptyList<ServiceHistoryResponse>()

        return ok(serviceHistory)
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
            vehicleApplicationService.getVehicleById(id.toLong())
                ?: throw ResourceNotFoundException("Vehicle", id)

            // Validate service history entry
            ValidationUtils.validateNotBlank(request.serviceType, "Service type")
            ValidationUtils.validateNotBlank(request.description, "Description")
            ValidationUtils.validatePositive(request.price, "Price")
            ValidationUtils.validateNotFutureDate(request.date, "Service date")

            // For now, create a mock response - this would need proper service history implementation
            val response = ServiceHistoryResponse(
                id = UUID.randomUUID().toString(),
                vehicleId = id,
                serviceType = request.serviceType,
                description = request.description,
                price = request.price,
                date = request.date.toString()
            )

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

        // For now, always return success - this would need proper service history implementation
        val deleted = true

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

        val vehicles = vehicleApplicationService.searchVehicles(
            make = make,
            model = model,
            licensePlate = licensePlate,
            vin = null,
            year = null,
            pageable = PageRequest.of(0, 1000)
        )

        val response = vehicles.content.map { VehicleMapper.toResponse(it) }
        return ok(response)
    }
}