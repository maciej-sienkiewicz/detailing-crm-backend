package com.carslab.crm.modules.clients.api.responses

import com.carslab.crm.modules.clients.domain.ClientDetailResponse
import com.carslab.crm.modules.clients.domain.VehicleServiceInfoResponse
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val updatedAt: LocalDateTime?,
    val lastVisitDate: LocalDate?
)

data class ClientExpandedResponse(
    val id: String,
    @JsonProperty("firstName")
    val firstName: String,
    @JsonProperty("lastName")
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val company: String? = null,
    @JsonProperty("taxId")
    val taxId: String? = null,

    // Statystyki - dopasowane do oczekiwań frontendu
    @JsonProperty("totalVisits")
    val totalVisits: Int,
    @JsonProperty("totalTransactions")
    val totalTransactions: Int,
    @JsonProperty("abandonedSales")
    val abandonedSales: Int,
    @JsonProperty("totalRevenue")
    val totalRevenue: Double,
    @JsonProperty("contactAttempts")
    val contactAttempts: Int,
    @JsonProperty("lastVisitDate")
    val lastVisitDate: String? = null,

    val notes: String? = null,
    val vehicles: List<String> = emptyList()
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun fromDomain(client: ClientDetailResponse): ClientExpandedResponse {
            return ClientExpandedResponse(
                id = client.id.toString(),
                firstName = client.firstName,
                lastName = client.lastName,
                email = client.email,
                phone = client.phone,
                address = client.address?.takeIf { it.isNotBlank() },
                company = client.company?.takeIf { it.isNotBlank() },
                taxId = client.taxId?.takeIf { it.isNotBlank() },


                // Mapowanie statystyk z domeny
                totalVisits = client.statistics.visitCount.toInt(),
                totalTransactions = 0,
                abandonedSales = 0, // Dodaj to pole do domeny
                totalRevenue = client.statistics.totalRevenue.toDouble(),
                lastVisitDate = client.statistics.lastVisitDate?.format(dateFormatter),

                notes = client.notes?.takeIf { it.isNotBlank() },
                contactAttempts = 0,
                vehicles = client.vehicles.map { it.id.toString() }
            )
        }
    }
}

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

data class VehicleTableResponse(
    val id: Long,
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String?,
    val vin: String?,
    val mileage: Long?,
    val owners: List<VehicleOwnerSummary>,
    val visitCount: Long,
    val lastVisitDate: LocalDateTime?,
    val totalRevenue: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Summary information about vehicle owner
 */
data class VehicleOwnerSummary(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String?,
    val phone: String?
)

/**
 * Response DTO for company-wide vehicle statistics
 */
data class VehicleCompanyStatisticsResponse(
    val totalVehicles: Long,
    val premiumVehicles: Long,
    val visitRevenueMedian: BigDecimal,
    val totalRevenue: BigDecimal,
    val averageRevenuePerVehicle: BigDecimal,
    val mostActiveVehicle: MostActiveVehicleInfo?,
    val calculatedAt: LocalDateTime
)

/**
 * Information about the most active vehicle
 */
data class MostActiveVehicleInfo(
    val id: Long,
    val make: String,
    val model: String,
    val licensePlate: String,
    val visitCount: Long,
    val totalRevenue: BigDecimal
)