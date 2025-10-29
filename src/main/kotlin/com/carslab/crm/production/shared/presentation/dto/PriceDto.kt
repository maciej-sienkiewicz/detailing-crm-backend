package com.carslab.crm.production.shared.presentation.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class PriceDto(
    @field:NotNull(message = "Input price is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Input price cannot be negative")
    @JsonProperty("input_price")
    val inputPrice: BigDecimal,

    @field:NotNull(message = "Input type is required")
    @JsonProperty("input_type")
    val inputType: PriceTypeDto
)