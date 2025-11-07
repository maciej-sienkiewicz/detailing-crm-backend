package com.carslab.crm.production.modules.visits.domain.models.entities

import com.carslab.crm.production.modules.visits.domain.models.enums.ServiceApprovalStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal

data class VisitService(
    val id: String,
    val name: String,
    val basePrice: PriceValueObject,
    val quantity: Long,
    val discount: ServiceDiscount? = null,
    val approvalStatus: ServiceApprovalStatus = ServiceApprovalStatus.PENDING,
    val note: String? = null
) {
    /**
     * Calculates the final price after applying quantity and discount
     */
    fun calculateFinalPrice(): PriceValueObject {
        val quantityMultiplier = BigDecimal.valueOf(quantity)
        val priceAfterQuantity = basePrice.multiply(quantityMultiplier)

        return if (discount != null) {
            discount.applyTo(priceAfterQuantity, 23.toBigDecimal())
        } else {
            priceAfterQuantity
        }
    }

    /**
     * Gets the total brutto amount for this service
     */
    fun getTotalBrutto(): BigDecimal {
        return calculateFinalPrice().priceBrutto
    }

    /**
     * Gets the total netto amount for this service
     */
    fun getTotalNetto(): BigDecimal {
        return calculateFinalPrice().priceNetto
    }

    /**
     * Gets the total tax amount for this service
     */
    fun getTotalTax(): BigDecimal {
        return calculateFinalPrice().taxAmount
    }
}

data class VisitListService(
    val id: String,
    val name: String,
    val finalPriceNetto: BigDecimal,
    val finalPriceBrutto: BigDecimal
)