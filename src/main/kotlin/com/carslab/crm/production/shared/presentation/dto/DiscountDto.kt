package com.carslab.crm.production.shared.presentation.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * DTO dla rabatu w żądaniach API.
 *
 * Frontend przesyła INTENCJĘ rabatową, backend wykonuje wszystkie obliczenia.
 */
data class DiscountDto(
    @field:NotNull(message = "Discount type is required")
    @JsonProperty("discount_type")
    val discountType: DiscountTypeDto,

    @field:NotNull(message = "Discount value is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Discount value cannot be negative")
    @JsonProperty("discount_value")
    val discountValue: BigDecimal,
)

/**
 * DTO dla rabatu w odpowiedziach API.
 * Zawiera pełne informacje o zastosowanym rabacie i jego efekcie.
 */
data class DiscountResponseDto(
    @JsonProperty("discount_type")
    val discountType: DiscountTypeDto,

    @JsonProperty("discount_value")
    val discountValue: BigDecimal,

    @JsonProperty("savings")
    val savings: PriceResponseDto
)