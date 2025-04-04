package com.carslab.crm.api.controller

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.mapper.ServiceMapper
import com.carslab.crm.api.model.commands.CreateServiceRecipeCommand
import com.carslab.crm.api.model.response.ServiceResponse
import com.carslab.crm.domain.ServiceFacade
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = ["*"])
@Tag(name = "Services", description = "Service management endpoints")
class ServiceController(
    private val serviceFacade: ServiceFacade
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get all services", description = "Retrieves all services")
    fun getAllServices(): ResponseEntity<List<ServiceResponse>> {
        logger.info("Getting all services")

        val services = serviceFacade.getAllServices()
        val response = services.map { ServiceMapper.toResponse(it) }
        return ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by ID", description = "Retrieves a service by its ID")
    fun getServiceById(
        @Parameter(description = "Service ID", required = true) @PathVariable id: String
    ): ResponseEntity<ServiceResponse> {
        logger.info("Getting service by ID: $id")

        val service = serviceFacade.getServiceById(id.toLong())
            ?: throw ResourceNotFoundException("Service", id)

        return ok(ServiceMapper.toResponse(service))
    }

    @PostMapping
    @Operation(summary = "Create a new service", description = "Creates a new service with the provided information")
    fun createService(@Valid @RequestBody request: CreateServiceRecipeCommand): ResponseEntity<ServiceRecipeId> {
        logger.info("Received request to create new service: ${request.name}")

        try {
            val domainService = ServiceMapper.toDomain(request)
            val createdService = serviceFacade.createService(domainService)

            return created(createdService)
        } catch (e: Exception) {
            return logAndRethrow("Error creating service", e)
        }
    }
}