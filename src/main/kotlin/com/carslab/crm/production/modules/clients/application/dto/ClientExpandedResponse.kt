package com.carslab.crm.production.modules.clients.application.dto

import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import com.fasterxml.jackson.annotation.JsonProperty
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
    val totalRevenue: Double,
    @JsonProperty("lastVisitDate")
    val lastVisitDate: String? = null,
    val notes: String? = null
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(clientWithStats: ClientWithStatistics): ClientExpandedResponse {
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
                totalRevenue = clientWithStats.statistics?.totalRevenue?.toDouble() ?: 0.0,
                lastVisitDate = clientWithStats.statistics?.lastVisitDate?.format(DATE_FORMATTER),
                notes = clientWithStats.client.notes
            )
        }
    }
}