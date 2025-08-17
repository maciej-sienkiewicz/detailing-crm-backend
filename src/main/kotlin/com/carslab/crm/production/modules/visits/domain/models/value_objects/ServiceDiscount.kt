package com.carslab.crm.production.modules.visits.domain.models.value_objects

import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import java.math.BigDecimal

data class ServiceDiscount(
    val type: DiscountType,
    val value: BigDecimal
) {
    fun applyTo(amount: BigDecimal): BigDecimal {
        return when (type) {
            DiscountType.PERCENTAGE -> amount.multiply(BigDecimal.ONE.subtract(value.divide(BigDecimal.valueOf(100))))
            DiscountType.AMOUNT -> amount.subtract(value)
            DiscountType.FIXED_PRICE -> value
        }
    }
}