package com.carslab.crm.production.modules.vehicles.application.dto

import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class VehicleResponse(
    val id: String,
    val make: String,
    val model: String,
    val year: Int?,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(vehicle: Vehicle): VehicleResponse {
            return VehicleResponse(
                id = vehicle.id.value.toString(),
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                color = vehicle.color,
                vin = vehicle.vin,
                mileage = vehicle.mileage,
                displayName = vehicle.displayName,
                createdAt = vehicle.createdAt,
                updatedAt = vehicle.updatedAt
            )
        }
    }
}

data class VehicleWithStatisticsResponse(
    val vehicle: VehicleResponse,
    val statistics: VehicleStatisticsResponse?
) {
    companion object {
        fun from(vehicle: Vehicle, statistics: VehicleStatistics?): VehicleWithStatisticsResponse {
            return VehicleWithStatisticsResponse(
                vehicle = VehicleResponse.from(vehicle),
                statistics = statistics?.let { VehicleStatisticsResponse.from(it) }
            )
        }
    }
}

data class VehicleStatisticsResponse(
    @JsonProperty("visit_count")
    val visitCount: Long,
    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,
    @JsonProperty("last_visit_date")
    val lastVisitDate: LocalDateTime?
) {
    companion object {
        fun from(statistics: VehicleStatistics): VehicleStatisticsResponse {
            return VehicleStatisticsResponse(
                visitCount = statistics.visitCount,
                totalRevenue = statistics.totalRevenue,
                lastVisitDate = statistics.lastVisitDate
            )
        }
    }
}