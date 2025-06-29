package com.carslab.crm.modules.visits.api.mappers

import com.carslab.crm.modules.visits.application.commands.models.valueobjects.*
import com.carslab.crm.modules.visits.api.commands.CreateServiceCommand

object ProtocolApiMappers {
    
    fun toCreateServiceCommand(apiCommand: CreateServiceCommand): com.carslab.crm.modules.visits.application.commands.models.valueobjects.CreateServiceCommand {
        return CreateServiceCommand(
            name = apiCommand.name,
            basePrice = apiCommand.price,
            quantity = apiCommand.quantity,
            discountType = apiCommand.discountType?.name,
            discountValue = apiCommand.discountValue,
            finalPrice = apiCommand.finalPrice,
            approvalStatus = apiCommand.approvalStatus?.name ?: "PENDING",
            note = apiCommand.note
        )
    }
}