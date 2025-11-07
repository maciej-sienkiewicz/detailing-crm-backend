package com.carslab.crm.production.shared.domain.value_objects

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value Object reprezentujący rabat.
 *
 * Jest autorytatywnym źródłem prawdy dla wszystkich obliczeń rabatowych.
 * Gwarantuje spójność i niezmienność danych finansowych.
 *
 * INVARIANTY:
 * - Wartość rabatu musi być nieujemna
 * - Dla PERCENT: wartość musi być w zakresie 0-100
 * - Dla kwotowych: wartość musi być >= 0
 * - Finalna cena nigdy nie może być ujemna
 */
data class DiscountValueObject(
    val type: DiscountType,
    val value: BigDecimal,
) {
    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
        private val ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE)
        private val ONE_HUNDRED = BigDecimal(100).setScale(SCALE, ROUNDING_MODE)

        /**
         * Tworzy rabat bez walidacji - tylko dla użytku wewnętrznego.
         * Użyj metod create* zamiast tego.
         */
        private fun createUnsafe(
            type: DiscountType,
            value: BigDecimal,
        ): DiscountValueObject {
            return DiscountValueObject(
                type = type,
                value = value.setScale(SCALE, ROUNDING_MODE),
            )
        }

        /**
         * Tworzy rabat procentowy.
         * @param percentage Wartość procentowa (0-100)
         */
        fun createPercent(percentage: BigDecimal, reason: String? = null): DiscountValueObject {
            val normalizedValue = percentage.setScale(SCALE, ROUNDING_MODE)
            require(normalizedValue >= ZERO) {
                "Percentage discount cannot be negative: $percentage"
            }
            require(normalizedValue <= ONE_HUNDRED) {
                "Percentage discount cannot exceed 100%: $percentage"
            }
            return createUnsafe(DiscountType.PERCENT, normalizedValue)
        }

        /**
         * Tworzy rabat - obniżka o stałą kwotę brutto.
         */
        fun createFixedAmountOffBrutto(
            amount: BigDecimal,
            reason: String? = null
        ): DiscountValueObject {
            val normalizedValue = amount.setScale(SCALE, ROUNDING_MODE)
            require(normalizedValue >= ZERO) {
                "Discount amount cannot be negative: $amount"
            }
            return createUnsafe(DiscountType.FIXED_AMOUNT_OFF_BRUTTO, normalizedValue)
        }

        /**
         * Tworzy rabat - obniżka o stałą kwotę netto.
         */
        fun createFixedAmountOffNetto(
            amount: BigDecimal,
        ): DiscountValueObject {
            val normalizedValue = amount.setScale(SCALE, ROUNDING_MODE)
            require(normalizedValue >= ZERO) {
                "Discount amount cannot be negative: $amount"
            }
            return createUnsafe(DiscountType.FIXED_AMOUNT_OFF_NETTO, normalizedValue)
        }

        /**
         * Tworzy rabat - ustawienie sztywnej ceny końcowej brutto.
         */
        fun createFixedFinalBrutto(
            finalPrice: BigDecimal,
        ): DiscountValueObject {
            val normalizedValue = finalPrice.setScale(SCALE, ROUNDING_MODE)
            require(normalizedValue >= ZERO) {
                "Final price cannot be negative: $finalPrice"
            }
            return createUnsafe(DiscountType.FIXED_FINAL_BRUTTO, normalizedValue)
        }

        /**
         * Tworzy rabat - ustawienie sztywnej ceny końcowej netto.
         */
        fun createFixedFinalNetto(
            finalPrice: BigDecimal,
        ): DiscountValueObject {
            val normalizedValue = finalPrice.setScale(SCALE, ROUNDING_MODE)
            require(normalizedValue >= ZERO) {
                "Final price cannot be negative: $finalPrice"
            }
            return createUnsafe(DiscountType.FIXED_FINAL_NETTO, normalizedValue)
        }

        /**
         * Tworzy rabat z surowych danych (np. z bazy).
         * Waliduje poprawność danych.
         */
        fun fromRaw(
            type: DiscountType,
            value: BigDecimal,
        ): DiscountValueObject {
            return when (type) {
                DiscountType.PERCENT -> createPercent(value)
                DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> createFixedAmountOffBrutto(value)
                DiscountType.FIXED_AMOUNT_OFF_NETTO -> createFixedAmountOffNetto(value)
                DiscountType.FIXED_FINAL_BRUTTO -> createFixedFinalBrutto(value)
                DiscountType.FIXED_FINAL_NETTO -> createFixedFinalNetto(value,)
            }
        }
    }

    init {
        // Walidacja niezmienników
        val normalizedValue = value.setScale(SCALE, ROUNDING_MODE)
        require(normalizedValue >= ZERO) {
            "Discount value cannot be negative: $value for type $type"
        }

        when (type) {
            DiscountType.PERCENT -> require(normalizedValue <= ONE_HUNDRED) {
                "Percentage discount cannot exceed 100%: $value"
            }
            else -> {
                // Dla pozostałych typów wystarczy sprawdzenie nieujemności
            }
        }
    }

    /**
     * Oblicza finalną cenę po zastosowaniu rabatu.
     *
     * KRYTYCZNE: To jest JEDYNE miejsce w systemie gdzie obliczamy ceny z rabatem.
     * Wszelkie inne obliczenia muszą delegować do tej metody.
     *
     * @param basePrice Cena bazowa (przed rabatem)
     * @param vatRate Stawka VAT w procentach (np. 23)
     * @return Finalna cena po rabacie
     */
    fun applyTo(basePrice: PriceValueObject, vatRate: Int): PriceValueObject {
        return when (type) {
            DiscountType.PERCENT -> applyPercentDiscount(basePrice, vatRate)
            DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> applyFixedAmountOffBrutto(basePrice, vatRate)
            DiscountType.FIXED_AMOUNT_OFF_NETTO -> applyFixedAmountOffNetto(basePrice, vatRate)
            DiscountType.FIXED_FINAL_BRUTTO -> applyFixedFinalBrutto(vatRate)
            DiscountType.FIXED_FINAL_NETTO -> applyFixedFinalNetto(vatRate)
        }
    }

    /**
     * Oblicza kwotę zaoszczędzoną przez rabat.
     * @return Różnica między ceną bazową a finalną (zawsze >= 0)
     */
    fun calculateSavings(basePrice: PriceValueObject, vatRate: Int): PriceValueObject {
        val finalPrice = applyTo(basePrice, vatRate)
        val savings = basePrice.subtract(finalPrice)

        // Zabezpieczenie przed ujemnymi oszczędnościami (gdy rabat faktycznie podnosi cenę)
        return if (savings.priceBrutto >= ZERO && savings.priceNetto >= ZERO) {
            savings
        } else {
            PriceValueObject(ZERO, ZERO, ZERO)
        }
    }

    /**
     * Sprawdza czy rabat może być zastosowany do danej ceny bazowej.
     * Niektóre rabaty mogą być niepoprawne (np. obniżka większa niż cena bazowa).
     */
    fun canBeAppliedTo(basePrice: PriceValueObject): Boolean {
        return try {
            val finalPrice = when (type) {
                DiscountType.PERCENT -> {
                    // Procentowy zawsze można zastosować
                    return true
                }
                DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> {
                    basePrice.priceBrutto >= value
                }
                DiscountType.FIXED_AMOUNT_OFF_NETTO -> {
                    basePrice.priceNetto >= value
                }
                DiscountType.FIXED_FINAL_BRUTTO -> {
                    // Można ustawić dowolną finalną cenę (nawet wyższą)
                    return true
                }
                DiscountType.FIXED_FINAL_NETTO -> {
                    // Można ustawić dowolną finalną cenę (nawet wyższą)
                    return true
                }
            }
            finalPrice
        } catch (e: Exception) {
            false
        }
    }

    // ============ METODY PRYWATNE - IMPLEMENTACJA LOGIKI RABATOWEJ ============

    private fun applyPercentDiscount(basePrice: PriceValueObject, vatRate: Int): PriceValueObject {
        // Obliczamy procent rabatu jako mnożnik (np. 10% = 0.90)
        val percentMultiplier = ONE_HUNDRED.subtract(value)
            .divide(ONE_HUNDRED, 4, ROUNDING_MODE)

        // Stosujemy rabat do ceny brutto
        val discountedBrutto = basePrice.priceBrutto
            .multiply(percentMultiplier)
            .setScale(SCALE, ROUNDING_MODE)

        // Rekonstruujemy cenę z nową wartością brutto
        return PriceValueObject.createFromInput(
            inputValue = discountedBrutto,
            inputType = PriceType.BRUTTO,
            vatRate = vatRate
        )
    }

    private fun applyFixedAmountOffBrutto(
        basePrice: PriceValueObject,
        vatRate: Int
    ): PriceValueObject {
        // Odejmujemy kwotę od ceny brutto
        val discountedBrutto = basePrice.priceBrutto.subtract(value)
            .setScale(SCALE, ROUNDING_MODE)

        // Zabezpieczenie przed ujemną ceną
        val finalBrutto = if (discountedBrutto < ZERO) ZERO else discountedBrutto

        return PriceValueObject.createFromInput(
            inputValue = finalBrutto,
            inputType = PriceType.BRUTTO,
            vatRate = vatRate
        )
    }

    private fun applyFixedAmountOffNetto(
        basePrice: PriceValueObject,
        vatRate: Int
    ): PriceValueObject {
        // Odejmujemy kwotę od ceny netto
        val discountedNetto = basePrice.priceNetto.subtract(value)
            .setScale(SCALE, ROUNDING_MODE)

        // Zabezpieczenie przed ujemną ceną
        val finalNetto = if (discountedNetto < ZERO) ZERO else discountedNetto

        return PriceValueObject.createFromInput(
            inputValue = finalNetto,
            inputType = PriceType.NETTO,
            vatRate = vatRate
        )
    }

    private fun applyFixedFinalBrutto(vatRate: Int): PriceValueObject {
        // Ustawiamy sztywną cenę końcową brutto
        return PriceValueObject.createFromInput(
            inputValue = value,
            inputType = PriceType.BRUTTO,
            vatRate = vatRate
        )
    }

    private fun applyFixedFinalNetto(vatRate: Int): PriceValueObject {
        // Ustawiamy sztywną cenę końcową netto
        return PriceValueObject.createFromInput(
            inputValue = value,
            inputType = PriceType.NETTO,
            vatRate = vatRate
        )
    }

    /**
     * Zwraca czytelny opis rabatu dla użytkownika.
     */
    fun getDescription(): String {
        return when (type) {
            DiscountType.PERCENT -> "${value}% zniżki"
            DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> "Obniżka o ${value} zł brutto"
            DiscountType.FIXED_AMOUNT_OFF_NETTO -> "Obniżka o ${value} zł netto"
            DiscountType.FIXED_FINAL_BRUTTO -> "Cena finalna: ${value} zł brutto"
            DiscountType.FIXED_FINAL_NETTO -> "Cena finalna: ${value} zł netto"
        }
    }
}