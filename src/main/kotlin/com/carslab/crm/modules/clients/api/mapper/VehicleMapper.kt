package com.carslab.crm.modules.clients.api.mapper

import com.carslab.crm.modules.clients.api.responses.VehicleResponse
import com.carslab.crm.modules.clients.domain.VehicleDetailResponse
import com.carslab.crm.modules.clients.domain.VehicleSummaryResponse

object VehicleMapper {

    fun toResponse(vehicleDetail: VehicleDetailResponse): VehicleResponse {
        return VehicleResponse(
            id = vehicleDetail.id,
            make = vehicleDetail.make,
            model = vehicleDetail.model,
            year = vehicleDetail.year,
            licensePlate = vehicleDetail.licensePlate,
            color = vehicleDetail.color,
            vin = vehicleDetail.vin,
            mileage = vehicleDetail.mileage,
            displayName = vehicleDetail.displayName,
            serviceInfo = vehicleDetail.serviceInfo,
            createdAt = vehicleDetail.createdAt,
            updatedAt = vehicleDetail.updatedAt
        )
    }

    fun toResponse(vehicleSummary: VehicleSummaryResponse): VehicleResponse {
        return VehicleResponse(
            id = vehicleSummary.id,
            make = vehicleSummary.make,
            model = vehicleSummary.model,
            year = null, // Not available in summary
            licensePlate = vehicleSummary.licensePlate,
            color = null,
            vin = null,
            mileage = null,
            displayName = vehicleSummary.displayName,
            serviceInfo = null, // Not available in summary
            createdAt = null, // Not available in summary
            updatedAt = null // Not available in summary
        )
    }
}