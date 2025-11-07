package com.carslab.crm.production.modules.visits.domain.models.value_objects

import com.carslab.crm.production.shared.domain.value_objects.DiscountType
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
        private val ONE_HUNDRED = BigDecimal(100)
    }

    init {
        if (value < BigDecimal.ZERO) {
            throw IllegalArgumentException("Discount value cannot be negative: $value")
        }
        if (type == DiscountType.PERCENT && value > ONE_HUNDRED) {
            throw IllegalArgumentException("Percentage discount cannot exceed 100%: $value")
        }
    }

    /**
     * Applies this discount to the given price.
     * @param price Cena bazowa do zastosowania rabatu.
     * @param vatRate Stawka VAT w formacie np. 0.23 dla 23%. Wymagana dla przeliczeń Netto <-> Brutto.
     */
    fun applyTo(price: PriceValueObject, vatRate: BigDecimal): PriceValueObject {
        val vatMultiplier = BigDecimal.ONE.add(vatRate.divide(100.toBigDecimal()))

        return when (type) {
            DiscountType.PERCENT -> applyPercentageDiscount(price)
            DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> applyFixedAmountOffBrutto(price, vatMultiplier)
            DiscountType.FIXED_AMOUNT_OFF_NETTO -> applyFixedAmountOffNetto(price, vatMultiplier)
            DiscountType.FIXED_FINAL_BRUTTO -> applyFixedFinalBrutto(vatMultiplier)
            DiscountType.FIXED_FINAL_NETTO -> applyFixedFinalNetto(vatMultiplier)
        }
    }

    // --- Typy Rabatów ---

    private fun applyPercentageDiscount(price: PriceValueObject): PriceValueObject {
        val discountMultiplier = BigDecimal.ONE.subtract(
            value.divide(ONE_HUNDRED, 4, ROUNDING_MODE)
        )

        val finalNetto = price.priceNetto.multiply(discountMultiplier)
        val finalBrutto = price.priceBrutto.multiply(discountMultiplier)

        return createPriceValueObject(finalNetto, finalBrutto)
    }

    private fun applyFixedAmountOffBrutto(price: PriceValueObject, vatMultiplier: BigDecimal): PriceValueObject {
        val finalBrutto = (price.priceBrutto.subtract(value)).max(BigDecimal.ZERO)
        val finalNetto = finalBrutto.divide(vatMultiplier, 4, ROUNDING_MODE)

        return createPriceValueObject(finalNetto, finalBrutto)
    }

    private fun applyFixedAmountOffNetto(price: PriceValueObject, vatMultiplier: BigDecimal): PriceValueObject {
        val finalNetto = (price.priceNetto.subtract(value)).max(BigDecimal.ZERO)
        val finalBrutto = finalNetto.multiply(vatMultiplier)

        return createPriceValueObject(finalNetto, finalBrutto)
    }

    private fun applyFixedFinalBrutto(vatMultiplier: BigDecimal): PriceValueObject {
        val finalBrutto = value
        val finalNetto = finalBrutto.divide(vatMultiplier, 4, ROUNDING_MODE)

        return createPriceValueObject(finalNetto, finalBrutto)
    }

    private fun applyFixedFinalNetto(vatMultiplier: BigDecimal): PriceValueObject {
        val finalNetto = value
        val finalBrutto = finalNetto.multiply(vatMultiplier)

        return createPriceValueObject(finalNetto, finalBrutto)
    }

    // --- Metoda pomocnicza do ujednolicenia zaokrągleń ---

    private fun createPriceValueObject(netto: BigDecimal, brutto: BigDecimal): PriceValueObject {
        // Zaokrąglamy Brutto i Netto do 2 miejsc
        val roundedBrutto = brutto.setScale(SCALE, ROUNDING_MODE)
        val roundedNetto = netto.setScale(SCALE, ROUNDING_MODE)

        // Obliczamy Tax jako różnicę zaokrąglonych wartości
        val calculatedTax = roundedBrutto.subtract(roundedNetto)

        // Zapewnia, że każda wartość jest zaokrąglona do 2 miejsc
        return PriceValueObject(
            priceNetto = roundedNetto,
            priceBrutto = roundedBrutto,
            taxAmount = calculatedTax.setScale(SCALE, ROUNDING_MODE)
        )
    }
}