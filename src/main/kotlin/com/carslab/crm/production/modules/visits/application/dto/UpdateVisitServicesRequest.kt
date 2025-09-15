package com.carslab.crm.production.modules.visits.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

data class UpdateVisitServicesRequest(
    @field:Valid
    @field:NotEmpty(message = "Services list cannot be empty")
    @field:Size(max = 50, message = "Cannot update more than 50 services at once")
    val services: List<UpdateVisitServiceItemRequest>
)

data class UpdateVisitServiceItemRequest(
    @field:NotBlank(message = "Service name is required")
    @field:Size(max = 100, message = "Service name cannot exceed 100 characters")
    val name: String,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    val price: BigDecimal,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Long,

    @JsonProperty("discount_type")
    val discountType: String? = null,

    @JsonProperty("discount_value")
    val discountValue: BigDecimal? = null,

    @JsonProperty("final_price")
    val finalPrice: BigDecimal? = null,

    @JsonProperty("approval_status")
    val approvalStatus: String? = "PENDING",

    val note: String? = null
)