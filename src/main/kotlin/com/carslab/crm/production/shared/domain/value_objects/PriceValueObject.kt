package com.carslab.crm.production.shared.domain.value_objects

import java.math.BigDecimal
import java.math.RoundingMode

data class PriceValueObject(
    val priceNetto: BigDecimal,
    val priceBrutto: BigDecimal,
    val taxAmount: BigDecimal
) {
    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP

        fun createFromInput(inputValue: BigDecimal, inputType: PriceType, vatRate: Int): PriceValueObject {
            val currentVatMultiplier = BigDecimal.ONE.add(
                BigDecimal(vatRate).divide(BigDecimal(100), 4, ROUNDING_MODE)
            )

            val validatedInputValue = inputValue.setScale(SCALE, ROUNDING_MODE)

            val priceNetto: BigDecimal
            val priceBrutto: BigDecimal
            val taxAmount: BigDecimal

            if (inputType == PriceType.BRUTTO) {
                priceBrutto = validatedInputValue

                val nettoPrecise = priceBrutto.divide(currentVatMultiplier, 4, ROUNDING_MODE)
                val vatPrecise = priceBrutto.subtract(nettoPrecise)

                taxAmount = vatPrecise.setScale(SCALE, ROUNDING_MODE)
                priceNetto = priceBrutto.subtract(taxAmount)

            } else {
                priceNetto = validatedInputValue

                val bruttoPrecise = priceNetto.multiply(currentVatMultiplier)

                priceBrutto = bruttoPrecise.setScale(SCALE, ROUNDING_MODE)
                taxAmount = priceBrutto.subtract(priceNetto)
            }

            validateConsistency(priceNetto, taxAmount, priceBrutto)

            return PriceValueObject(priceNetto, priceBrutto, taxAmount)
        }

        private fun validateConsistency(priceNetto: BigDecimal, taxAmount: BigDecimal, priceBrutto: BigDecimal) {
            if (priceNetto.add(taxAmount).compareTo(priceBrutto) != 0) {
                throw IllegalStateException(
                    "Price consistency error: Netto ($priceNetto) + VAT ($taxAmount) must equal Brutto ($priceBrutto)"
                )
            }
        }
    }

    init {
        if (priceNetto < BigDecimal.ZERO) {
            throw IllegalArgumentException("Netto price cannot be negative: $priceNetto")
        }
        if (priceBrutto < BigDecimal.ZERO) {
            throw IllegalArgumentException("Brutto price cannot be negative: $priceBrutto")
        }
        if (taxAmount < BigDecimal.ZERO) {
            throw IllegalArgumentException("Tax amount cannot be negative: $taxAmount")
        }
        if (priceNetto.add(taxAmount).compareTo(priceBrutto) != 0) {
            throw IllegalArgumentException(
                "Price inconsistency: Netto ($priceNetto) + VAT ($taxAmount) must equal Brutto ($priceBrutto)"
            )
        }
    }

    fun multiply(multiplier: BigDecimal): PriceValueObject {
        return PriceValueObject(
            priceNetto = priceNetto.multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            priceBrutto = priceBrutto.multiply(multiplier).setScale(SCALE, ROUNDING_MODE),
            taxAmount = taxAmount.multiply(multiplier).setScale(SCALE, ROUNDING_MODE)
        )
    }

    fun add(other: PriceValueObject): PriceValueObject {
        return PriceValueObject(
            priceNetto = priceNetto.add(other.priceNetto),
            priceBrutto = priceBrutto.add(other.priceBrutto),
            taxAmount = taxAmount.add(other.taxAmount)
        )
    }

    fun subtract(other: PriceValueObject): PriceValueObject {
        return PriceValueObject(
            priceNetto = priceNetto.subtract(other.priceNetto),
            priceBrutto = priceBrutto.subtract(other.priceBrutto),
            taxAmount = taxAmount.subtract(other.taxAmount)
        )
    }
}