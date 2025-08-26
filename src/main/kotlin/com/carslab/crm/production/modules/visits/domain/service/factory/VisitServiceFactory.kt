package com.carslab.crm.production.modules.visits.domain.service.factory

import com.carslab.crm.production.modules.visits.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class VisitServiceFactory {

    fun createService(command: CreateServiceCommand): VisitService {
        val discount = createDiscount(command.discountType, command.discountValue)
        val finalPrice = calculateFinalPrice(command.basePrice, command.quantity, discount, command.finalPrice)

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    fun updateService(command: UpdateServiceCommand): VisitService {
        val discount = createDiscount(command.discountType, command.discountValue)
        val finalPrice = calculateFinalPrice(command.basePrice, command.quantity, discount, command.finalPrice)

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    private fun createDiscount(
        discountType: com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType?,
        discountValue: BigDecimal?
    ): ServiceDiscount? {
        return if (discountType != null && discountValue != null) {
            ServiceDiscount(discountType, discountValue)
        } else null
    }

    private fun calculateFinalPrice(
        basePrice: BigDecimal,
        quantity: Long,
        discount: ServiceDiscount?,
        explicitFinalPrice: BigDecimal?
    ): BigDecimal {
        if (explicitFinalPrice != null) {
            return explicitFinalPrice
        }

        val totalBase = basePrice.multiply(BigDecimal.valueOf(quantity))

        return discount?.applyTo(totalBase) ?: totalBase
    }
}