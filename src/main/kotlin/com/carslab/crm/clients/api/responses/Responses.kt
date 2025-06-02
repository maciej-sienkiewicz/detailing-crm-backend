package com.carslab.crm.clients.api.responses

import com.carslab.crm.clients.domain.ClientStatisticsResponse
import com.carslab.crm.clients.domain.VehicleSummaryResponse
import com.carslab.crm.clients.domain.VehicleServiceInfoResponse
import java.math.BigDecimal
import java.time.LocalDateTime

data class ClientResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class ClientExpandedResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    val taxId: String?,
    val notes: String?,
    val statistics: ClientStatisticsResponse,
    val vehicles: List<VehicleSummaryResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class VehicleResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val displayName: String,
    val serviceInfo: VehicleServiceInfoResponse?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class VehicleStatisticsResponse(
    val visitNo: Long,
    val servicesNo: Int,
    val totalRevenue: BigDecimal
)

data class VehicleOwnerResponse(
    val id: Long,
    val fullName: String
)

data class ClientStatisticsResponse(
    val totalVisits: Long,
    val totalRevenue: BigDecimal,
    val vehicleNo: Long
)

data class ServiceHistoryResponse(
    val id: String,
    val vehicleId: String,
    val serviceType: String,
    val description: String,
    val price: BigDecimal,
    val date: String
)