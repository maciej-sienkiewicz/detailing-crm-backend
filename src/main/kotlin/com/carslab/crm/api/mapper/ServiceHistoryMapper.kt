package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.request.ServiceHistoryRequest
import com.carslab.crm.api.model.response.ServiceHistoryResponse
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.clients.domain.model.VehicleId
import java.time.LocalDateTime

object ServiceHistoryMapper {

    fun toDomain(request: ServiceHistoryRequest): ServiceHistory {
        val now = LocalDateTime.now()

        return ServiceHistory(
            id = request.id?.let { ServiceHistoryId(it) } ?: ServiceHistoryId.generate(),
            vehicleId = VehicleId(request.vehicleId?.toLong() ?: throw IllegalArgumentException("Vehicle ID is required")),
            date = request.date,
            serviceType = request.serviceType,
            description = request.description,
            price = request.price,
            protocolId = request.protocolId,
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun toResponse(serviceHistory: ServiceHistory): ServiceHistoryResponse {
        return ServiceHistoryResponse(
            id = serviceHistory.id.value,
            vehicleId = serviceHistory.vehicleId.value.toString(),
            date = serviceHistory.date,
            serviceType = serviceHistory.serviceType,
            description = serviceHistory.description,
            price = serviceHistory.price,
            protocolId = serviceHistory.protocolId,
            createdAt = serviceHistory.audit.createdAt,
            updatedAt = serviceHistory.audit.updatedAt
        )
    }
}