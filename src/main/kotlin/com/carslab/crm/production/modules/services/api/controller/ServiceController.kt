package com.carslab.crm.production.modules.services.api.controller

import com.carslab.crm.production.modules.services.application.dto.CreateServiceRequest
import com.carslab.crm.production.modules.services.application.dto.ServiceResponse
import com.carslab.crm.production.modules.services.application.dto.UpdateServiceRequest
import com.carslab.crm.production.modules.services.application.service.ServiceCommandService
import com.carslab.crm.production.modules.services.application.service.ServiceQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Service management endpoints")
class ServiceController(
    private val commandService: ServiceCommandService,
    private val queryService: ServiceQueryService
) : BaseController() {

    @GetMapping
    @HttpMonitored(endpoint = "GET_/api/services")
    @Operation(summary = "Get all services", description = "Retrieves all active services for current company")
    fun getAllServices(): ResponseEntity<List<ServiceResponse>> {
        logger.info("Getting all services")

        val services = queryService.getServicesForCurrentCompany()
        return ok(services)
    }

    @GetMapping("/{id}")
    @HttpMonitored(endpoint = "GET_/api/services/{id}")
    @Operation(summary = "Get service by ID", description = "Retrieves an active service by its ID")
    fun getServiceById(
        @Parameter(description = "Service ID", required = true) @PathVariable id: String
    ): ResponseEntity<ServiceResponse> {
        logger.info("Getting service by ID: {}", id)

        val service = queryService.getService(id)
        return ok(service)
    }

    @PostMapping
    @HttpMonitored(endpoint = "POST_/api/services")
    @Operation(summary = "Create a new service", description = "Creates a new service")
    fun createService(@Valid @RequestBody request: CreateServiceRequest): ResponseEntity<ServiceResponse> {
        logger.info("Creating new service: {}", request.name)

        val service = commandService.createService(request)
        return created(service)
    }

    @PutMapping("/{id}")
    @HttpMonitored(endpoint = "PUT_/api/services/{id}")
    @Operation(summary = "Update service", description = "Updates an existing service")
    fun updateService(
        @Parameter(description = "Service ID", required = true) @PathVariable id: String,
        @Valid @RequestBody request: UpdateServiceRequest
    ): ResponseEntity<ServiceResponse> {
        logger.info("Updating service: {}", id)

        val service = commandService.updateService(id, request)
        return ok(service)
    }

    @DeleteMapping("/{id}")
    @HttpMonitored(endpoint = "DELETE_/api/services/{id}")
    @Operation(summary = "Delete service", description = "Deactivates a service")
    fun deleteService(
        @Parameter(description = "Service ID", required = true) @PathVariable id: String
    ): ResponseEntity<Void> {
        logger.info("Deleting service: {}", id)

        commandService.deleteService(id)
        return ResponseEntity.noContent().build()
    }
}