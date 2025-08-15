package com.carslab.crm.production.modules.services.application.dto

import com.carslab.crm.production.modules.services.domain.model.Service
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ServiceResponse(
    val id: String,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    @JsonProperty("vat_rate")
    val vatRate: Int,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(service: Service): ServiceResponse {
            return ServiceResponse(
                id = service.id.value,
                name = service.name,
                description = service.description,
                price = service.price,
                vatRate = service.vatRate,
                createdAt = service.createdAt,
                updatedAt = service.updatedAt
            )
        }
    }
}