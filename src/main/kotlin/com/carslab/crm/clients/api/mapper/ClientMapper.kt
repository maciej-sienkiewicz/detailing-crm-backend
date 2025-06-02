package com.carslab.crm.clients.api.mapper

import com.carslab.crm.clients.api.responses.*
import com.carslab.crm.clients.domain.ClientDetailResponse

object ClientMapper {

    fun toResponse(clientDetail: ClientDetailResponse): ClientResponse {
        return ClientResponse(
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
            createdAt = clientDetail.createdAt,
            updatedAt = clientDetail.updatedAt
        )
    }

    fun toExpandedResponse(clientDetail: ClientDetailResponse): ClientExpandedResponse {
        return ClientExpandedResponse(
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
            statistics = clientDetail.statistics,
            vehicles = clientDetail.vehicles,
            createdAt = clientDetail.createdAt,
            updatedAt = clientDetail.updatedAt
        )
    }
}