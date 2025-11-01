package com.carslab.crm.production.modules.clients.application.dto

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ClientResponse(
    val id: String,
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("last_name")
    val lastName: String,
    @JsonProperty("full_name")
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String?,
    val company: String?,
    @JsonProperty("tax_id")
    val taxId: String?,
    val notes: String?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(client: Client): ClientResponse {
            return ClientResponse(
                id = client.id.value.toString(),
                firstName = client.firstName,
                lastName = client.lastName,
                fullName = client.fullName,
                email = client.email,
                phone = client.phone,
                address = client.address,
                company = client.company,
                taxId = client.taxId,
                notes = client.notes,
                createdAt = client.createdAt,
                updatedAt = client.updatedAt
            )
        }
    }
}

data class ClientWithStatisticsResponse(
    val client: ClientResponse,
    val statistics: ClientStatisticsResponse?
) {
    companion object {
        fun from(client: Client, statistics: ClientStatistics?): ClientWithStatisticsResponse {
            return ClientWithStatisticsResponse(
                client = ClientResponse.from(client),
                statistics = statistics?.let { ClientStatisticsResponse.from(it) }
            )
        }
    }
}

data class ClientStatisticsResponse(
    @JsonProperty("visit_count")
    val visitCount: Long,
    @JsonProperty("total_revenue")
    val totalRevenue: PriceResponseDto,
    @JsonProperty("vehicle_count")
    val vehicleCount: Long,
    @JsonProperty("last_visit_date")
    val lastVisitDate: LocalDateTime?
) {
    companion object {
        fun from(statistics: ClientStatistics): ClientStatisticsResponse {
            return ClientStatisticsResponse(
                visitCount = statistics.visitCount,
                totalRevenue = PriceResponseDto(
                    statistics.totalRevenue.priceNetto,
                    statistics.totalRevenue.priceBrutto,
                    statistics.totalRevenue.taxAmount,
                ),
                vehicleCount = statistics.vehicleCount,
                lastVisitDate = statistics.lastVisitDate
            )
        }
    }
}