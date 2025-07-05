package com.carslab.crm.modules.visits.application.mappers

import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.modules.visits.api.mappers.ServiceMappers
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand
import com.carslab.crm.modules.visits.application.commands.models.valueobjects.UpdateServiceCommand
import org.springframework.stereotype.Component

@Component
class ProtocolServiceMapper {
    fun toCreateServiceModel(updateCommand: UpdateServiceCommand): CreateServiceModel {
        return ServiceMappers.toCreateServiceModel(
            CreateServiceCommand(
                name = updateCommand.name,
                basePrice = updateCommand.basePrice,
                quantity = updateCommand.quantity,
                discountType = updateCommand.discountType,
                discountValue = updateCommand.discountValue,
                finalPrice = updateCommand.finalPrice,
                approvalStatus = updateCommand.approvalStatus,
                note = updateCommand.note
            )
        )
    }
}