package com.carslab.crm.production.modules.clients.application.dto

import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

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
    @JsonProperty("totalVisits")
    val totalVisits: Int,
    @JsonProperty("totalRevenue")
    val totalRevenue: PriceResponseDto,
    @JsonProperty("lastVisitDate")
    val lastVisitDate: String? = null,
    val notes: String? = null,
    val vehicles: List<Long> = emptyList(),
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(clientWithStats: ClientWithStatistics, ): ClientExpandedResponse {
            return ClientExpandedResponse(
                id = clientWithStats.client.id.value.toString(),
                firstName = clientWithStats.client.firstName,
                lastName = clientWithStats.client.lastName,
                email = clientWithStats.client.email,
                phone = clientWithStats.client.phone,
                address = clientWithStats.client.address,
                company = clientWithStats.client.company,
                taxId = clientWithStats.client.taxId,
                totalVisits = clientWithStats.statistics?.visitCount?.toInt() ?: 0,
                totalRevenue = PriceResponseDto(
                    priceNetto = clientWithStats.statistics?.totalRevenue?.priceNetto ?: BigDecimal.ZERO,
                    priceBrutto = clientWithStats.statistics?.totalRevenue?.priceBrutto ?: BigDecimal.ZERO,
                    taxAmount = clientWithStats.statistics?.totalRevenue?.taxAmount ?: BigDecimal.ZERO,
                ),
                lastVisitDate = clientWithStats.statistics?.lastVisitDate?.format(DATE_FORMATTER),
                notes = clientWithStats.client.notes,
                vehicles = clientWithStats.vehicleIds
            )
        }
    }
}