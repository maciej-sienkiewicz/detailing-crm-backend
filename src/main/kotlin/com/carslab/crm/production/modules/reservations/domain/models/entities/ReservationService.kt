package com.carslab.crm.production.modules.reservations.domain.models.entities

import com.carslab.crm.production.shared.domain.value_objects.DiscountValueObject
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal

/**
 * Usługa w rezerwacji.
 *
 * Enkapsuluje logikę cenową pojedynczej usługi wraz z rabatami.
 * Jest autorytatywnym źródłem prawdy dla obliczeń finansowych usługi.
 *
 * INVARIANTY:
 * - Nazwa nie może być pusta
 * - Ilość musi być > 0
 * - Cena bazowa musi być poprawna
 * - Rabat (jeśli istnieje) musi być możliwy do zastosowania
 */
data class ReservationService(
    val id: String,
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long = 1,
    val discount: DiscountValueObject? = null,
    val note: String? = null
) {
    companion object {
        private const val VAT_RATE = 23 // Domyślna stawka VAT dla usług detailingowych
    }

    init {
        require(name.isNotBlank()) { "Service name cannot be blank" }
        require(quantity > 0) { "Service quantity must be positive" }

        // Walidacja rabatu - jeśli istnieje, musi być możliwy do zastosowania
        discount?.let { disc ->
            val priceAfterQuantity = basePrice.multiply(BigDecimal.valueOf(quantity))
            require(disc.canBeAppliedTo(priceAfterQuantity)) {
                "Discount ${disc.type} with value ${disc.value} cannot be applied to base price ${basePrice.priceBrutto} * $quantity"
            }
        }
    }

    /**
     * Oblicza finalną cenę JEDNEJ jednostki usługi (basePrice z rabatem).
     *
     * KRYTYCZNE: To jest autorytatywne miejsce obliczania ceny jednostkowej z rabatem.
     */
    fun calculateUnitPrice(): PriceValueObject {
        return if (discount != null) {
            discount.applyTo(basePrice, VAT_RATE)
        } else {
            basePrice
        }
    }

    /**
     * Oblicza finalną cenę całkowitą (unitPrice * quantity).
     *
     * KRYTYCZNE: To jest autorytatywne miejsce obliczania ceny całkowitej.
     * Kolejność operacji: basePrice -> rabat -> mnożenie przez ilość
     */
    fun calculateFinalPrice(): PriceValueObject {
        val unitPrice = calculateUnitPrice()
        val quantityMultiplier = BigDecimal.valueOf(quantity)
        return unitPrice.multiply(quantityMultiplier)
    }

    /**
     * Oblicza oszczędności wynikające z rabatu (dla całkowitej ilości).
     */
    fun calculateTotalSavings(): PriceValueObject? {
        if (discount == null) return null

        val priceWithoutDiscount = basePrice.multiply(BigDecimal.valueOf(quantity))
        return discount.calculateSavings(priceWithoutDiscount, VAT_RATE)
    }

    // ============ CONVENIENCE METHODS ============

    fun getTotalBrutto(): BigDecimal = calculateFinalPrice().priceBrutto
    fun getTotalNetto(): BigDecimal = calculateFinalPrice().priceNetto
    fun getTotalTax(): BigDecimal = calculateFinalPrice().taxAmount

    fun hasDiscount(): Boolean = discount != null

    /**
     * Tworzy kopię usługi z nowym rabatem.
     * Waliduje czy rabat może być zastosowany.
     */
    fun withDiscount(newDiscount: DiscountValueObject?): ReservationService {
        return copy(discount = newDiscount)
    }

    /**
     * Tworzy kopię usługi bez rabatu.
     */
    fun withoutDiscount(): ReservationService {
        return copy(discount = null)
    }
}