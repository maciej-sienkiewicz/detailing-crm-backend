package com.carslab.crm.production.modules.visits.domain.factory

import com.carslab.crm.production.modules.visits.domain.command.CreateServiceCommand
import com.carslab.crm.production.modules.visits.domain.command.UpdateServiceCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.DiscountType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class VisitServiceFactory {

    fun createService(command: CreateServiceCommand): VisitService {
        val discount = createDiscount(command.discountType, command.discountValue)

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    fun updateService(command: UpdateServiceCommand): VisitService {
        val discount = createDiscount(command.discountType, command.discountValue)

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = command.basePrice,
            quantity = command.quantity,
            discount = discount,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    private fun createDiscount(
        discountType: DiscountType?,
        discountValue: BigDecimal?
    ): ServiceDiscount? {
        return if (discountType != null && discountValue != null) {
            ServiceDiscount(discountType, discountValue)
        } else null
    }
}