package com.carslab.crm.production.modules.visits.infrastructure.utils

import java.math.BigDecimal
import java.math.RoundingMode

object CalculationUtils {

    private const val DEFAULT_SCALE = 2
    private val DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP
    private val VAT_RATE = BigDecimal("23") // 23%
    private val VAT_MULTIPLIER = BigDecimal("1.23") // 1 + 23/100

    /**
     * Konwertuje Double na BigDecimal z odpowiednią precyzją
     */
    fun doubleToBigDecimal(value: Double?): BigDecimal {
        return value?.let {
            BigDecimal.valueOf(it).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
        } ?: BigDecimal.ZERO
    }

    /**
     * Konwertuje String na BigDecimal z odpowiednią precyzją
     */
    fun stringToBigDecimal(value: String?): BigDecimal {
        return value?.let {
            try {
                BigDecimal(it).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        } ?: BigDecimal.ZERO
    }

    /**
     * Konwertuje Any na BigDecimal z odpowiednią precyzją
     */
    fun anyToBigDecimal(value: Any?): BigDecimal {
        return when (value) {
            null -> BigDecimal.ZERO
            is BigDecimal -> value.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
            is Double -> doubleToBigDecimal(value)
            is Float -> doubleToBigDecimal(value.toDouble())
            is Int -> BigDecimal(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
            is Long -> BigDecimal(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
            is String -> stringToBigDecimal(value)
            else -> stringToBigDecimal(value.toString())
        }
    }

    /**
     * Oblicza kwotę netto z kwoty brutto
     */
    fun calculateNetAmount(grossAmount: BigDecimal): BigDecimal {
        return grossAmount.divide(VAT_MULTIPLIER, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Oblicza kwotę brutto z kwoty netto
     */
    fun calculateGrossAmount(netAmount: BigDecimal): BigDecimal {
        return netAmount.multiply(VAT_MULTIPLIER).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Oblicza VAT z kwoty netto
     */
    fun calculateVatAmount(netAmount: BigDecimal): BigDecimal {
        return netAmount.multiply(VAT_RATE).divide(BigDecimal("100"), DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Oblicza VAT z różnicy między brutto a netto
     */
    fun calculateVatFromGrossNet(grossAmount: BigDecimal, netAmount: BigDecimal): BigDecimal {
        return grossAmount.subtract(netAmount).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Waliduje czy sumy się zgadzają z tolerancją błędu
     */
    fun amountsMatch(amount1: BigDecimal, amount2: BigDecimal, tolerance: BigDecimal = BigDecimal("0.01")): Boolean {
        return amount1.subtract(amount2).abs() <= tolerance
    }

    /**
     * Oblicza finalną cenę z ceną bazową, ilością i zniżką
     */
    fun calculateFinalPrice(
        basePrice: BigDecimal,
        quantity: Long,
        discountType: String?,
        discountValue: BigDecimal?
    ): BigDecimal {
        val totalBase = basePrice.multiply(BigDecimal(quantity))

        if (discountType == null || discountValue == null) {
            return totalBase.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
        }

        return when (discountType.uppercase()) {
            "PERCENTAGE" -> {
                val discountAmount = totalBase.multiply(discountValue).divide(BigDecimal("100"), DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
                totalBase.subtract(discountAmount)
            }
            "AMOUNT" -> {
                totalBase.subtract(discountValue).max(BigDecimal.ZERO)
            }
            "FIXED_PRICE" -> {
                discountValue.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
            }
            else -> totalBase
        }.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE)
    }

    /**
     * Bezpiecznie porównuje dwa BigDecimal z tolerancją
     */
    fun safeCompare(value1: BigDecimal?, value2: BigDecimal?, tolerance: BigDecimal = BigDecimal("0.01")): Boolean {
        val v1 = value1 ?: BigDecimal.ZERO
        val v2 = value2 ?: BigDecimal.ZERO
        return amountsMatch(v1, v2, tolerance)
    }
}