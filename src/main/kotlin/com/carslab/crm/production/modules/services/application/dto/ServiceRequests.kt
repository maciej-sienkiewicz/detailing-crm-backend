package com.carslab.crm.production.modules.services.application.dto

import com.carslab.crm.production.shared.presentation.dto.PriceDto
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateServiceRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name cannot exceed 100 characters")
    val name: String,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null,

    @field:Valid
    @field:NotNull(message = "Price is required")
    val price: PriceDto,

    @field:NotNull(message = "VAT rate is required")
    @JsonProperty("vat_rate")
    val vatRate: Int
)

data class UpdateServiceRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name cannot exceed 100 characters")
    val name: String,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null,

    @field:Valid
    @field:NotNull(message = "Price is required")
    val price: PriceDto,

    @field:NotNull(message = "VAT rate is required")
    @JsonProperty("vat_rate")
    val vatRate: Int
)