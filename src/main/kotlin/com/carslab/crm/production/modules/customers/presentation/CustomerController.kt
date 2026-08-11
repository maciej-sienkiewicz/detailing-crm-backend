package com.carslab.crm.production.modules.customers.presentation

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.associations.infrastructure.repository.ClientVehicleAssociationJpaRepository
import com.carslab.crm.production.modules.vehicles.infrastructure.repository.VehicleJpaRepository
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers V1", description = "Customer management V1 endpoints")
class CustomerController(
    private val associationJpaRepository: ClientVehicleAssociationJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val securityContext: SecurityContext
) : BaseController() {

    @GetMapping("/{customerId}/vehicles")
    @HttpMonitored(endpoint = "GET_/api/v1/customers/{customerId}/vehicles")
    @Operation(summary = "Get customer vehicles", description = "Retrieves vehicles associated with a customer, optionally including deleted ones")
    fun getCustomerVehicles(
        @Parameter(description = "Customer ID", required = true) @PathVariable customerId: String,
        @Parameter(description = "Include deleted vehicles") @RequestParam(defaultValue = "false") includeDeleted: Boolean
    ): ResponseEntity<List<CustomerVehicleResponse>> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting vehicles for customer: {}, includeDeleted: {}", customerId, includeDeleted)

        val vehicleIds = if (includeDeleted) {
            associationJpaRepository.findAllVehicleIdsByClientId(customerId.toLong())
        } else {
            associationJpaRepository.findByClientIdAndEndDateIsNull(customerId.toLong())
                .map { it.vehicleId }
        }

        if (vehicleIds.isEmpty()) {
            return ok(emptyList())
        }

        val vehicles = vehicleJpaRepository.findAllById(vehicleIds)
            .filter { it.companyId == companyId }
            .let { entities -> if (includeDeleted) entities else entities.filter { it.active } }

        val response = vehicles.map { entity ->
            CustomerVehicleResponse(
                id = entity.id.toString(),
                brand = entity.make,
                model = entity.model,
                year = entity.year,
                licensePlate = entity.licensePlate,
                color = entity.color,
                active = entity.active
            )
        }

        logger.info("Returning {} vehicles for customer: {}", response.size, customerId)
        return ok(response)
    }
}

data class CustomerVehicleResponse(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int?,
    @JsonProperty("licensePlate")
    val licensePlate: String,
    val color: String?,
    val active: Boolean
)
