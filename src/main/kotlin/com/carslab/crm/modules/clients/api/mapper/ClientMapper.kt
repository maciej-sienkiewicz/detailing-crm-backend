package com.carslab.crm.modules.clients.api.mapper

import com.carslab.crm.modules.clients.api.responses.*
import com.carslab.crm.modules.clients.domain.ClientDetailResponse

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
            updatedAt = clientDetail.updatedAt,
            lastVisitDate = clientDetail.statistics.lastVisitDate?.toLocalDate(),
        )
    }
}