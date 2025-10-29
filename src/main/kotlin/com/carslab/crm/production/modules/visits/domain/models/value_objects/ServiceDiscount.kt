package com.carslab.crm.production.modules.visits.domain.models.value_objects

import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal
import java.math.RoundingMode

data class ServiceDiscount(
    val type: DiscountType,
    val value: BigDecimal
) {
    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
    }

    init {
        if (value < BigDecimal.ZERO) {
            throw IllegalArgumentException("Discount value cannot be negative: $value")
        }
        if (type == DiscountType.PERCENTAGE && value > BigDecimal(100)) {
            throw IllegalArgumentException("Percentage discount cannot exceed 100%: $value")
        }
    }

    /**
     * Applies this discount to the given price
     */
    fun applyTo(price: PriceValueObject): PriceValueObject {
        return when (type) {
            DiscountType.PERCENTAGE -> applyPercentageDiscount(price)
            DiscountType.AMOUNT -> applyAmountDiscount(price)
            DiscountType.FIXED_PRICE -> applyFixedPrice(price)
        }
    }

    private fun applyPercentageDiscount(price: PriceValueObject): PriceValueObject {
        val discountMultiplier = BigDecimal.ONE.subtract(
            value.divide(BigDecimal(100), 4, ROUNDING_MODE)
        )

        val newNetto = price.priceNetto.multiply(discountMultiplier).setScale(SCALE, ROUNDING_MODE)
        val newBrutto = price.priceBrutto.multiply(discountMultiplier).setScale(SCALE, ROUNDING_MODE)
        val newTax = newBrutto.subtract(newNetto)

        return PriceValueObject(newNetto, newBrutto, newTax)
    }

    private fun applyAmountDiscount(price: PriceValueObject): PriceValueObject {
        // Discount is applied proportionally to netto and brutto
        val ratio = if (price.priceBrutto > BigDecimal.ZERO) {
            price.priceNetto.divide(price.priceBrutto, 4, ROUNDING_MODE)
        } else {
            BigDecimal.ZERO
        }

        val discountNetto = value.multiply(ratio).setScale(SCALE, ROUNDING_MODE)
        val discountBrutto = value.setScale(SCALE, ROUNDING_MODE)

        val newNetto = (price.priceNetto.subtract(discountNetto)).max(BigDecimal.ZERO)
        val newBrutto = (price.priceBrutto.subtract(discountBrutto)).max(BigDecimal.ZERO)
        val newTax = newBrutto.subtract(newNetto)

        return PriceValueObject(newNetto, newBrutto, newTax)
    }

    private fun applyFixedPrice(price: PriceValueObject): PriceValueObject {
        // Fixed price means the brutto price is set to the discount value
        // We need to recalculate netto and tax based on the VAT rate from original price
        val originalVatRate = if (price.priceNetto > BigDecimal.ZERO) {
            price.taxAmount.divide(price.priceNetto, 4, ROUNDING_MODE).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        val newBrutto = value.setScale(SCALE, ROUNDING_MODE)
        val vatMultiplier = BigDecimal.ONE.add(originalVatRate.divide(BigDecimal(100), 4, ROUNDING_MODE))
        val newNetto = newBrutto.divide(vatMultiplier, 4, ROUNDING_MODE).setScale(SCALE, ROUNDING_MODE)
        val newTax = newBrutto.subtract(newNetto)

        return PriceValueObject(newNetto, newBrutto, newTax)
    }
}