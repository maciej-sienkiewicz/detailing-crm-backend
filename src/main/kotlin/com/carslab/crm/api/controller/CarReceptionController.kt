package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.mapper.CarReceptionMapper
import com.carslab.crm.api.mapper.CarReceptionMapperExtension
import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.request.CarReceptionProtocolRequest
import com.carslab.crm.api.model.response.CarReceptionProtocolDetailResponse
import com.carslab.crm.api.model.response.CarReceptionProtocolListResponse
import com.carslab.crm.api.model.response.CarReceptionProtocolResponse
import com.carslab.crm.api.model.response.ClientProtocolHistoryResponse
import com.carslab.crm.domain.CarReceptionFacade
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import com.carslab.crm.infrastructure.util.ValidationUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/receptions")
@CrossOrigin(origins = ["*"])
@Tag(name = "Car Receptions", description = "Car reception protocol management endpoints")
class CarReceptionController(
    private val carReceptionFacade: CarReceptionFacade
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create a car reception protocol", description = "Creates a new car reception protocol for a vehicle")
    fun createCarReceptionProtocol(@Valid @RequestBody request: CarReceptionProtocolRequest): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Creating new car reception protocol for: ${request.ownerName}, vehicle: ${request.make} ${request.model}")

        try {
            // Validate request data
            validateCarReceptionRequest(request)

            // Convert request to domain model
            val domainProtocol = CarReceptionMapper.toDomain(request)

            // Create protocol using facade
            val createdProtocol = carReceptionFacade.createProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionMapper.toResponse(createdProtocol)

            logger.info("Successfully created car reception protocol with ID: ${response.id}")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating car reception protocol", e)
        }
    }

    @GetMapping("/list")
    @Operation(summary = "Get list of car reception protocols", description = "Retrieves a list of car reception protocols with optional filtering")
    fun getCarReceptionProtocolsList(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: com.carslab.crm.api.model.request.ProtocolStatus?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolListResponse>> {
        logger.info("Getting list of car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionMapper.mapStatus(it.name) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionMapper.toListResponse(it) }
        return ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all car reception protocols", description = "Retrieves all car reception protocols with optional filtering")
    fun getAllCarReceptionProtocols(
        @Parameter(description = "Client name to filter by") @RequestParam(required = false) clientName: String?,
        @Parameter(description = "License plate to filter by") @RequestParam(required = false) licensePlate: String?,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: com.carslab.crm.api.model.request.ProtocolStatus?,
        @Parameter(description = "Start date to filter by (ISO format)") @RequestParam(required = false) startDate: String?,
        @Parameter(description = "End date to filter by (ISO format)") @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<CarReceptionProtocolResponse>> {
        logger.info("Getting all car reception protocols with filters")

        val domainStatus = status?.let { CarReceptionMapper.mapStatus(it.name) }

        val protocols = carReceptionFacade.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = domainStatus,
            startDate = parseDateParam(startDate),
            endDate = parseDateParam(endDate)
        )

        val response = protocols.map { CarReceptionMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car reception protocol by ID", description = "Retrieves a specific car reception protocol by its ID")
    fun getCarReceptionProtocolById(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Getting car reception protocol by ID: $id")

        val protocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        return ok(CarReceptionMapper.toDetailResponse(protocol))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update car reception protocol", description = "Updates an existing car reception protocol")
    fun updateCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: CarReceptionProtocolRequest
    ): ResponseEntity<CarReceptionProtocolDetailResponse> {
        logger.info("Updating car reception protocol with ID: $id")

        // Verify protocol exists
        val existingProtocol = carReceptionFacade.getProtocolById(ProtocolId(id))
            ?: throw ResourceNotFoundException("Protocol", id)

        try {
            // Validate request data
            validateCarReceptionRequest(request)

            // Ensure ID in request matches path ID
            val requestWithId = request.apply { this.id = id }

            // Convert request to domain model
            val domainProtocol = CarReceptionMapper.toDomain(requestWithId).copy(
                audit = existingProtocol.audit.copy(
                    createdAt = existingProtocol.audit.createdAt
                )
            )

            // Update protocol using facade
            val updatedProtocol = carReceptionFacade.updateProtocol(domainProtocol)

            // Convert domain model to response
            val response = CarReceptionMapper.toDetailResponse(updatedProtocol)

            logger.info("Successfully updated car reception protocol with ID: $id")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating car reception protocol with ID: $id", e)
        }
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update protocol status", description = "Updates the status of a car reception protocol")
    fun updateProtocolStatus(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String,
        @RequestBody statusUpdate: StatusUpdateRequest
    ): ResponseEntity<CarReceptionProtocolResponse> {
        logger.info("Updating status of car reception protocol with ID: $id to ${statusUpdate.status}")

        if (statusUpdate.status.isNullOrBlank()) {
            throw ValidationException("Status cannot be empty")
        }

        try {
            val domainStatus = CarReceptionMapper.mapStatus(statusUpdate.status!!)
            val updatedProtocol = carReceptionFacade.changeStatus(ProtocolId(id), domainStatus)

            val response = CarReceptionMapper.toResponse(updatedProtocol)
            return ok(response)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid status value: ${statusUpdate.status}")
        } catch (e: Exception) {
            return logAndRethrow("Error updating status for protocol with ID: $id", e)
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete car reception protocol", description = "Deletes a car reception protocol by its ID")
    fun deleteCarReceptionProtocol(
        @Parameter(description = "Protocol ID", required = true) @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Deleting car reception protocol with ID: $id")

        val deleted = carReceptionFacade.deleteProtocol(ProtocolId(id))

        return if (deleted) {
            logger.info("Successfully deleted car reception protocol with ID: $id")
            ok(createSuccessResponse("Protocol successfully deleted", mapOf("protocolId" to id)))
        } else {
            logger.warn("Car reception protocol with ID: $id not found for deletion")
            throw ResourceNotFoundException("Protocol", id)
        }
    }

    @GetMapping("/{clientId}/protocols")
    @Operation(
        summary = "Get protocols for client",
        description = "Retrieves all car reception protocols for a specific client"
    )
    fun getProtocolsByClientId(
        @Parameter(description = "Client ID", required = true) @PathVariable clientId: Long,
        @Parameter(description = "Status to filter by") @RequestParam(required = false) status: ApiProtocolStatus?
    ): ResponseEntity<List<ClientProtocolHistoryResponse>> {
        logger.info("Getting protocols for client with ID: $clientId")


        val domainStatus = status?.let { CarReceptionMapperExtension.mapStatus(it) }

        val protocols = carReceptionFacade.searchProtocols(
            clientId = clientId,
            status = domainStatus
        )

        if (protocols.isEmpty()) {
            logger.info("No protocols found for client with ID: $clientId")
        } else {
            logger.info("Found ${protocols.size} protocols for client with ID: $clientId")
        }

        val response = protocols.map { CarReceptionMapper.toClientProtocolHistoryResponse(it) }
        return ok(response)
    }

    private fun validateCarReceptionRequest(request: CarReceptionProtocolRequest) {
        ValidationUtils.validateNotBlank(request.startDate, "Start date")

        if (request.startDate != null) {
            try {
                LocalDate.parse(request.startDate, DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                throw ValidationException("Invalid start date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        if (request.endDate != null) {
            try {
                val endDate = LocalDate.parse(request.endDate, DateTimeFormatter.ISO_DATE)
                val startDate = LocalDate.parse(request.startDate, DateTimeFormatter.ISO_DATE)

                if (endDate.isBefore(startDate)) {
                    throw ValidationException("End date cannot be before start date")
                }
            } catch (e: ValidationException) {
                throw e
            } catch (e: Exception) {
                throw ValidationException("Invalid end date format. Use ISO format (YYYY-MM-DD)")
            }
        }

        ValidationUtils.validateNotBlank(request.licensePlate, "License plate")
        ValidationUtils.validateNotBlank(request.make, "Vehicle make")
        ValidationUtils.validateNotBlank(request.model, "Vehicle model")
        ValidationUtils.validateNotBlank(request.ownerName, "Owner name")

        if (request.ownerName != null && request.email == null && request.phone == null) {
            throw ValidationException("At least one contact method (email or phone) is required")
        }

        if (request.email != null) {
            ValidationUtils.validateEmail(request.email, "Email")
        }

        if (request.phone != null) {
            ValidationUtils.validatePhone(request.phone, "Phone")
        }
    }

    private fun parseDateParam(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null

        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        } catch (e: Exception) {
            logger.warn("Invalid date format: $dateString")
            null
        }
    }
}

class StatusUpdateRequest {
    var status: String? = null

    constructor() {}
}