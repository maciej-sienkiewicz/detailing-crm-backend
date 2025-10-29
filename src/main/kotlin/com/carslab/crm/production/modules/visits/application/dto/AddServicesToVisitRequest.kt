package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.production.shared.presentation.dto.PriceDto
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.*

data class AddServicesToVisitRequest(
    @field:Valid
    @field:NotEmpty(message = "Services list cannot be empty")
    @field:Size(max = 20, message = "Cannot add more than 20 services at once")
    val services: List<AddServiceItemRequest>
)

data class AddServiceItemRequest(
    @JsonProperty("service_id")
    val serviceId: String? = null,

    @field:NotBlank(message = "Service name is required")
    @field:Size(max = 100, message = "Service name cannot exceed 100 characters")
    val name: String,

    @field:Valid
    @field:NotNull(message = "Price is required")
    val price: PriceDto,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Long,

    @JsonProperty("discount_type")
    val discountType: String? = null,

    @JsonProperty("discount_value")
    val discountValue: java.math.BigDecimal? = null,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String? = null,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null,

    @field:NotNull(message = "VAT rate is required")
    @JsonProperty("vat_rate")
    val vatRate: Int = 23
)