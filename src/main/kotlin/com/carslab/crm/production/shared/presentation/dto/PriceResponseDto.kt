package com.carslab.crm.production.shared.presentation.dto

import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class PriceResponseDto(
    @JsonProperty("price_netto")
    val priceNetto: BigDecimal,

    @JsonProperty("price_brutto")
    val priceBrutto: BigDecimal,

    @JsonProperty("tax_amount")
    val taxAmount: BigDecimal
) {
    companion object {
        fun from(priceValueObject: PriceValueObject): PriceResponseDto {
            return PriceResponseDto(
                priceNetto = priceValueObject.priceNetto,
                priceBrutto = priceValueObject.priceBrutto,
                taxAmount = priceValueObject.taxAmount
            )
        }
    }
}