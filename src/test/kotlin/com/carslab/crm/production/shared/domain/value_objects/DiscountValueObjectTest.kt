package com.carslab.crm.production.shared.domain.value_objects

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class DiscountValueObjectTest {

    private val VAT_RATE = 23

    @Test
    fun `should create percent discount correctly`() {
        val discount = DiscountValueObject.createPercent(
            percentage = BigDecimal("10.00"),
            reason = "Test discount"
        )

        assertEquals(DiscountType.PERCENT, discount.type)
        assertEquals(BigDecimal("10.00"), discount.value)
    }

    @Test
    fun `should reject negative percent discount`() {
        assertThrows<IllegalArgumentException> {
            DiscountValueObject.createPercent(BigDecimal("-5.00"))
        }
    }

    @Test
    fun `should reject percent discount over 100`() {
        assertThrows<IllegalArgumentException> {
            DiscountValueObject.createPercent(BigDecimal("101.00"))
        }
    }

    @Test
    fun `should apply 10 percent discount correctly`() {
        // Given: Cena bazowa 100 zł brutto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("10.00"))

        // When: Stosujemy rabat 10%
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena to 90 zł brutto
        assertEquals(BigDecimal("90.00"), finalPrice.priceBrutto)
        assertTrue(finalPrice.priceNetto < basePrice.priceNetto)
    }

    @Test
    fun `should apply fixed amount off brutto correctly`() {
        // Given: Cena bazowa 200 zł brutto, rabat 50 zł brutto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("200.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createFixedAmountOffBrutto(BigDecimal("50.00"))

        // When: Stosujemy rabat
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena to 150 zł brutto
        assertEquals(BigDecimal("150.00"), finalPrice.priceBrutto)
    }

    @Test
    fun `should apply fixed amount off netto correctly`() {
        // Given: Cena bazowa 123 zł brutto (100 netto + 23 VAT), rabat 20 zł netto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.NETTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createFixedAmountOffNetto(BigDecimal("20.00"))

        // When: Stosujemy rabat
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena netto to 80 zł (98.40 brutto)
        assertEquals(BigDecimal("80.00"), finalPrice.priceNetto)
    }

    @Test
    fun `should set fixed final brutto price correctly`() {
        // Given: Cena bazowa 200 zł brutto, chcemy ustawić finalną cenę na 100 zł brutto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("200.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createFixedFinalBrutto(BigDecimal("100.00"))

        // When: Stosujemy rabat
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena to dokładnie 100 zł brutto (niezależnie od ceny bazowej)
        assertEquals(BigDecimal("100.00"), finalPrice.priceBrutto)
    }

    @Test
    fun `should set fixed final netto price correctly`() {
        // Given: Cena bazowa dowolna, chcemy ustawić finalną cenę na 80 zł netto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("200.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createFixedFinalNetto(BigDecimal("80.00"))

        // When: Stosujemy rabat
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena to dokładnie 80 zł netto (98.40 brutto)
        assertEquals(BigDecimal("80.00"), finalPrice.priceNetto)
    }

    @Test
    fun `should calculate savings correctly for percent discount`() {
        // Given: Cena 100 zł brutto, rabat 20%
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("20.00"))

        // When: Obliczamy oszczędności
        val savings = discount.calculateSavings(basePrice, VAT_RATE)

        // Then: Oszczędności to 20 zł brutto
        assertEquals(BigDecimal("20.00"), savings.priceBrutto)
    }

    @Test
    fun `should prevent negative final price for fixed amount off`() {
        // Given: Cena bazowa 50 zł brutto, rabat 100 zł brutto (więcej niż cena)
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("50.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createFixedAmountOffBrutto(BigDecimal("100.00"))

        // When: Stosujemy rabat
        val finalPrice = discount.applyTo(basePrice, VAT_RATE)

        // Then: Finalna cena to 0 (zabezpieczenie przed ujemną ceną)
        assertEquals(BigDecimal.ZERO.setScale(2), finalPrice.priceBrutto)
    }

    @Test
    fun `should check if discount can be applied to base price`() {
        // Given: Cena bazowa 100 zł brutto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )

        // Then: Rabat 50 zł można zastosować
        val validDiscount = DiscountValueObject.createFixedAmountOffBrutto(BigDecimal("50.00"))
        assertTrue(validDiscount.canBeAppliedTo(basePrice))

        // And: Rabat procentowy zawsze można zastosować
        val percentDiscount = DiscountValueObject.createPercent(BigDecimal("50.00"))
        assertTrue(percentDiscount.canBeAppliedTo(basePrice))
    }

    @Test
    fun `should generate correct description for each discount type`() {
        val percentDiscount = DiscountValueObject.createPercent(
            BigDecimal("10.00"),
            "Stały klient"
        )
        assertTrue(percentDiscount.getDescription().contains("10.00%"))
        assertTrue(percentDiscount.getDescription().contains("Stały klient"))

        val fixedBrutto = DiscountValueObject.createFixedAmountOffBrutto(
            BigDecimal("50.00")
        )
        assertTrue(fixedBrutto.getDescription().contains("50.00"))
        assertTrue(fixedBrutto.getDescription().contains("brutto"))
    }

    @Test
    fun `should reject negative discount values`() {
        assertThrows<IllegalArgumentException> {
            DiscountValueObject.createFixedAmountOffBrutto(BigDecimal("-10.00"))
        }

        assertThrows<IllegalArgumentException> {
            DiscountValueObject.createFixedFinalNetto(BigDecimal("-5.00"))
        }
    }

    @Test
    fun `should create discount from raw data correctly`() {
        // Given: Surowe dane z bazy danych
        val discount = DiscountValueObject.fromRaw(
            type = DiscountType.PERCENT,
            value = BigDecimal("15.00"),
        )

        // Then: Rabat jest poprawnie utworzony
        assertEquals(DiscountType.PERCENT, discount.type)
        assertEquals(BigDecimal("15.00"), discount.value)
    }

    @Test
    fun `should validate raw data when creating from raw`() {
        // Given: Niepoprawne dane (procent > 100)
        assertThrows<IllegalArgumentException> {
            DiscountValueObject.fromRaw(
                type = DiscountType.PERCENT,
                value = BigDecimal("150.00")
            )
        }
    }
}