package com.carslab.crm.presentation.mapper

import com.carslab.crm.api.model.response.ClientExpandedResponse
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.api.model.request.ClientRequest
import com.carslab.crm.domain.model.ClientDetails
import com.carslab.crm.domain.model.ClientStats
import com.carslab.crm.api.model.response.ClientResponse

object ClientMapper {
    fun toDomain(request: ClientRequest): ClientDetails {
        return ClientDetails(
            id = request.id?.let { ClientId(it.toLong()) } ?: ClientId(),
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            phone = request.phone,
            address = request.address,
            company = request.company,
            taxId = request.taxId,
            notes = request.notes
        )
    }

    fun toResponse(client: ClientDetails): ClientResponse {
        return ClientResponse(
            id = client.id.value.toString(),
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt
        )
    }

    fun toExpandedResponse(client: ClientStats): ClientExpandedResponse {
        return ClientExpandedResponse(
            id = client.client.id.value.toString(),
            firstName = client.client.firstName,
            lastName = client.client.lastName,
            email = client.client.email,
            phone = client.client.phone,
            address = client.client.address,
            company = client.client.company,
            taxId = client.client.taxId,
            notes = client.client.notes,
            createdAt = client.client.audit.createdAt,
            updatedAt = client.client.audit.updatedAt,
            vehicles = client.vehicles.map { "${it.make} ${it.model} (${it.year})" },
            totalRevenue = client.stats?.gmv?.toDouble() ?: 0.0,
            totalVisits = client.stats?.visitNo?.toInt() ?: 0,
        )
    }

    fun toExpandedResponse(client: ClientDetails): ClientExpandedResponse {
        return ClientExpandedResponse(
            id = client.id.value.toString(),
            firstName = client.firstName,
            lastName = client.lastName,
            email = client.email,
            phone = client.phone,
            address = client.address,
            company = client.company,
            taxId = client.taxId,
            notes = client.notes,
            createdAt = client.audit.createdAt,
            updatedAt = client.audit.updatedAt,
            totalVisits = 0,
            totalRevenue = 0.0,
            vehicles = emptyList()
        )
    }
}