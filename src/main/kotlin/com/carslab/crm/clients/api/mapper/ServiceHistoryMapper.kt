package com.carslab.crm.clients.api.mapper

import com.carslab.crm.api.model.request.ServiceHistoryRequest
import com.carslab.crm.api.model.response.ServiceHistoryResponse
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import java.time.format.DateTimeFormatter

object ServiceHistoryMapper {

    fun toDomain(request: ServiceHistoryRequest): ServiceHistory {
        return ServiceHistory(
            id = ServiceHistoryId(),
            vehicleId = VehicleId(request.vehicleId!!.toLong()),
            serviceType = request.serviceType,
            description = request.description,
            price = request.price.toDouble(),
            date = request.date
        )
    }

    fun toResponse(serviceHistory: ServiceHistory): ServiceHistoryResponse {
        return ServiceHistoryResponse(
            id = serviceHistory.id.value,
            vehicleId = serviceHistory.vehicleId.value.toString(),
            serviceType = serviceHistory.serviceType,
            description = serviceHistory.description,
            price = serviceHistory.price.toBigDecimal(),
            date = serviceHistory.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
}
