package com.carslab.crm.production.modules.reservations.domain.models.entities

import com.carslab.crm.production.shared.domain.value_objects.DiscountValueObject
import com.carslab.crm.production.shared.domain.value_objects.PriceType
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ReservationServiceTest {

    private val VAT_RATE = 23

    @Test
    fun `should create service without discount`() {
        // Given
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )

        // When
        val service = ReservationService(
            id = "service-1",
            name = "Mycie standardowe",
            basePrice = basePrice,
            quantity = 2L,
            discount = null,
            note = "Test note"
        )

        // Then
        assertEquals("service-1", service.id)
        assertEquals("Mycie standardowe", service.name)
        assertEquals(2L, service.quantity)
        assertFalse(service.hasDiscount())
        assertNull(service.calculateTotalSavings())
    }

    @Test
    fun `should calculate unit price without discount`() {
        // Given: Cena bazowa 100 zł brutto
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 1L
        )

        // When
        val unitPrice = service.calculateUnitPrice()

        // Then: Unit price = base price (bez rabatu)
        assertEquals(basePrice.priceBrutto, unitPrice.priceBrutto)
        assertEquals(basePrice.priceNetto, unitPrice.priceNetto)
    }

    @Test
    fun `should calculate unit price with discount`() {
        // Given: Cena bazowa 100 zł brutto, rabat 10%
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("10.00"))
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 1L,
            discount = discount
        )

        // When
        val unitPrice = service.calculateUnitPrice()

        // Then: Unit price = 90 zł brutto (10% zniżki)
        assertEquals(BigDecimal("90.00"), unitPrice.priceBrutto)
    }

    @Test
    fun `should calculate final price without discount`() {
        // Given: Cena bazowa 100 zł brutto, ilość 3
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 3L
        )

        // When
        val finalPrice = service.calculateFinalPrice()

        // Then: Final price = 100 * 3 = 300 zł brutto
        assertEquals(BigDecimal("300.00"), finalPrice.priceBrutto)
    }

    @Test
    fun `should calculate final price with discount`() {
        // Given: Cena bazowa 100 zł brutto, rabat 10%, ilość 3
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("10.00"))
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 3L,
            discount = discount
        )

        // When
        val finalPrice = service.calculateFinalPrice()

        // Then: Final price = (100 * 0.9) * 3 = 270 zł brutto
        assertEquals(BigDecimal("270.00"), finalPrice.priceBrutto)
    }

    @Test
    fun `should calculate total savings correctly`() {
        // Given: Cena bazowa 100 zł brutto, rabat 20%, ilość 2
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("20.00"))
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 2L,
            discount = discount
        )

        // When
        val savings = service.calculateTotalSavings()

        // Then: Savings = (100 - 80) * 2 = 40 zł brutto
        assertNotNull(savings)
        assertEquals(BigDecimal("40.00"), savings!!.priceBrutto)
    }

    @Test
    fun `should return null savings when no discount`() {
        // Given: Usługa bez rabatu
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 2L,
            discount = null
        )

        // When
        val savings = service.calculateTotalSavings()

        // Then
        assertNull(savings)
    }

    @Test
    fun `should correctly report getTotalBrutto`() {
        // Given
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 2L
        )

        // When & Then
        assertEquals(BigDecimal("200.00"), service.getTotalBrutto())
    }

    @Test
    fun `should add discount to service`() {
        // Given: Usługa bez rabatu
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 1L
        )

        // When: Dodajemy rabat
        val discount = DiscountValueObject.createPercent(BigDecimal("15.00"))
        val serviceWithDiscount = service.withDiscount(discount)

        // Then
        assertTrue(serviceWithDiscount.hasDiscount())
        assertEquals(BigDecimal("15.00"), serviceWithDiscount.discount!!.value)
    }

    @Test
    fun `should remove discount from service`() {
        // Given: Usługa z rabatem
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val discount = DiscountValueObject.createPercent(BigDecimal("10.00"))
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 1L,
            discount = discount
        )

        // When: Usuwamy rabat
        val serviceWithoutDiscount = service.withoutDiscount()

        // Then
        assertFalse(serviceWithoutDiscount.hasDiscount())
        assertNull(serviceWithoutDiscount.discount)
    }

    @Test
    fun `should reject blank service name`() {
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )

        assertThrows<IllegalArgumentException> {
            ReservationService(
                id = "service-1",
                name = "  ",
                basePrice = basePrice,
                quantity = 1L
            )
        }
    }

    @Test
    fun `should reject zero or negative quantity`() {
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("100.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )

        assertThrows<IllegalArgumentException> {
            ReservationService(
                id = "service-1",
                name = "Test",
                basePrice = basePrice,
                quantity = 0L
            )
        }

        assertThrows<IllegalArgumentException> {
            ReservationService(
                id = "service-1",
                name = "Test",
                basePrice = basePrice,
                quantity = -1L
            )
        }
    }

    @Test
    fun `should validate discount can be applied during creation`() {
        // Given: Cena bazowa 50 zł brutto, rabat 100 zł brutto (więcej niż cena)
        val basePrice = PriceValueObject.createFromInput(
            inputValue = BigDecimal("50.00"),
            inputType = PriceType.BRUTTO,
            vatRate = VAT_RATE
        )
        val excessiveDiscount = DiscountValueObject.createFixedAmountOffBrutto(
            BigDecimal("100.00")
        )

        // When & Then: Tworzenie usługi z niepoprawnym rabatem nie powinno rzucić wyjątku
        // (rabat może obniżyć cenę do 0, ale nie poniżej)
        val service = ReservationService(
            id = "service-1",
            name = "Test",
            basePrice = basePrice,
            quantity = 1L,
            discount = excessiveDiscount
        )

        // Finalna cena to 0 (zabezpieczenie w DiscountValueObject)
        assertEquals(BigDecimal.ZERO.setScale(2), service.calculateFinalPrice().priceBrutto)
    }
}