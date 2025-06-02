package com.carslab.crm.presentation.mapper

import com.carslab.crm.api.model.response.ClientExpandedResponse
import com.carslab.crm.clients.domain.model.ClientResponse
import java.time.format.DateTimeFormatter


object ClientMapper {

    fun toResponse(client: ClientResponse): ClientResponse = ClientResponse(
        id = client.id,
        firstName = client.firstName,
        lastName = client.lastName,
        fullName = client.fullName,
        email = client.email,
        phone = client.phone,
        address = client.address,
        company = client.company,
        taxId = client.taxId,
        notes = client.notes,
        createdAt = client.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = client.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )

    fun toExpandedResponse(client: ClientResponse): ClientExpandedResponse = ClientExpandedResponse(
        id = client.id,
        firstName = client.firstName,
        lastName = client.lastName,
        fullName = client.fullName,
        email = client.email,
        phone = client.phone,
        address = client.address,
        company = client.company,
        taxId = client.taxId,
        notes = client.notes,
        createdAt = client.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = client.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        statistics = null // Will be populated separately if needed
    )

    fun toExpandedResponse(clientDetail: ClientDetailResponse): ClientExpandedResponse = ClientExpandedResponse(
        id = clientDetail.id,
        firstName = clientDetail.firstName,
        lastName = clientDetail.lastName,
        fullName = clientDetail.fullName,
        email = clientDetail.email,
        phone = clientDetail.phone,
        address = clientDetail.address,
        company = clientDetail.company,
        taxId = clientDetail.taxId,
        notes = clientDetail.notes,
        createdAt = clientDetail.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = clientDetail.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        statistics = ClientStatisticsDto(
            visitCount = clientDetail.statistics.visitCount,
            totalRevenue = clientDetail.statistics.totalRevenue,
            vehicleCount = clientDetail.statistics.vehicleCount,
            lastVisitDate = clientDetail.statistics.lastVisitDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    )
}
