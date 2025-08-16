package com.carslab.crm.production.modules.vehicles.application.dto

import com.carslab.crm.production.modules.vehicles.domain.model.EnhancedVehicle
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
        fun from(enhancedVehicle: EnhancedVehicle): VehicleWithStatisticsResponse {
            return VehicleWithStatisticsResponse(
                vehicle = VehicleResponse.from(enhancedVehicle.vehicle),
                statistics = enhancedVehicle.statistics?.let { VehicleStatisticsResponse.from(it) }
            )
        }

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

data class VehicleTableResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val owners: List<VehicleOwnerSummary>,
    @JsonProperty("visit_count")
    val visitCount: Long,
    @JsonProperty("last_visit_date")
    val lastVisitDate: LocalDateTime?,
    @JsonProperty("total_revenue")
    val totalRevenue: BigDecimal,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(enhancedVehicle: EnhancedVehicle): VehicleTableResponse {
            return VehicleTableResponse(
                id = enhancedVehicle.id.value,
                make = enhancedVehicle.make,
                model = enhancedVehicle.model,
                year = enhancedVehicle.year,
                licensePlate = enhancedVehicle.licensePlate,
                color = enhancedVehicle.color,
                vin = enhancedVehicle.vin,
                mileage = enhancedVehicle.mileage,
                owners = enhancedVehicle.owners.map { owner ->
                    VehicleOwnerSummary(
                        id = owner.id,
                        firstName = owner.firstName,
                        lastName = owner.lastName,
                        fullName = owner.fullName,
                        email = owner.email,
                        phone = owner.phone
                    )
                },
                visitCount = enhancedVehicle.visitCount,
                lastVisitDate = enhancedVehicle.lastVisitDate,
                totalRevenue = enhancedVehicle.totalRevenue,
                createdAt = enhancedVehicle.createdAt,
                updatedAt = enhancedVehicle.updatedAt
            )
        }
    }
}

data class VehicleOwnerSummary(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String?,
    val phone: String?
)