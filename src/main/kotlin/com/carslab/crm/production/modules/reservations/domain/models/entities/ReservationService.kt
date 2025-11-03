package com.carslab.crm.production.modules.reservations.domain.models.entities

import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal

/**
 * Usługa w rezerwacji
 * Podobna do VisitService, ale uproszczona
 */
data class ReservationService(
    val id: String,
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long = 1,
    val note: String? = null
) {
    init {
        require(name.isNotBlank()) { "Service name cannot be blank" }
        require(quantity > 0) { "Service quantity must be positive" }
    }

    /**
     * Oblicza finalną cenę (basePrice * quantity)
     */
    fun calculateFinalPrice(): PriceValueObject {
        val quantityMultiplier = BigDecimal.valueOf(quantity)
        return basePrice.multiply(quantityMultiplier)
    }

    fun getTotalBrutto(): BigDecimal = calculateFinalPrice().priceBrutto
    fun getTotalNetto(): BigDecimal = calculateFinalPrice().priceNetto
    fun getTotalTax(): BigDecimal = calculateFinalPrice().taxAmount
}