package com.carslab.crm.production.modules.clients.presentation

import com.carslab.crm.production.modules.clients.application.dto.CustomerDetailResponse
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.shared.observability.annotations.HttpMonitored
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers V1", description = "Customer management endpoints - API V1")
class CustomerV1Controller(
    private val clientQueryService: ClientQueryService
) : BaseController() {

    @GetMapping("/{customerId}/detail")
    @HttpMonitored(endpoint = "GET_/api/v1/customers/{customerId}/detail")
    @Operation(
        summary = "Get customer detail",
        description = "Retrieves detailed information about a customer including statistics, marketing consents, and loyalty tier"
    )
    fun getCustomerDetail(
        @Parameter(description = "Customer ID", required = true) @PathVariable customerId: String
    ): ResponseEntity<CustomerDetailResponse> {
        logger.info("Getting customer detail for customer ID: $customerId")

        val response = clientQueryService.getCustomerDetail(customerId)

        logger.info("Successfully retrieved customer detail for customer ID: $customerId")
        return ok(response)
    }
}
