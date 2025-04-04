package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.commands.CreateServiceRecipeCommand
import com.carslab.crm.api.model.response.ServiceResponse
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.view.ServiceRecipeView
import java.time.LocalDateTime

object ServiceMapper {

    fun toDomain(request: CreateServiceRecipeCommand): CreateServiceRecipeModel {
        val now = LocalDateTime.now()
        return CreateServiceRecipeModel(
            name = request.name,
            description = request.description,
            price = request.price,
            vatRate = request.vatRate,
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun toResponse(domain: ServiceRecipeView): ServiceResponse {
        return ServiceResponse(
            id = domain.id.value,
            name = domain.name,
            description = domain.description,
            price = domain.price,
            vatRate = domain.vatRate,
            createdAt = domain.audit.createdAt,
            updatedAt = domain.audit.updatedAt
        )
    }
}