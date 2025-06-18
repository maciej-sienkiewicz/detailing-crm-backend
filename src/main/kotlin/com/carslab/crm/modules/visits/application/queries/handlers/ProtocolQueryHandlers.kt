// src/main/kotlin/com/carslab/crm/modules/visits/application/queries/handlers/ProtocolQueryHandlers.kt
package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.infrastructure.persistence.read.ProtocolReadRepository
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.stereotype.Service

@Service
class GetProtocolByIdQueryHandler(
    private val protocolReadRepository: ProtocolReadRepository
) : QueryHandler<GetProtocolByIdQuery, ProtocolDetailReadModel?> {

    override fun handle(query: GetProtocolByIdQuery): ProtocolDetailReadModel? {
        return protocolReadRepository.findDetailById(query.protocolId)
    }
}

@Service
class SearchProtocolsQueryHandler(
    private val protocolReadRepository: ProtocolReadRepository
) : QueryHandler<SearchProtocolsQuery, PaginatedResponse<ProtocolListReadModel>> {

    override fun handle(query: SearchProtocolsQuery): PaginatedResponse<ProtocolListReadModel> {
        return protocolReadRepository.searchProtocols(
            clientName = query.clientName,
            clientId = query.clientId,
            licensePlate = query.licensePlate,
            make = query.make,
            status = query.status,
            startDate = query.startDate,
            endDate = query.endDate,
            page = query.page,
            size = query.size
        )
    }
}

@Service
class GetProtocolCountersQueryHandler(
    private val protocolReadRepository: ProtocolReadRepository
) : QueryHandler<GetProtocolCountersQuery, ProtocolCountersReadModel> {

    override fun handle(query: GetProtocolCountersQuery): ProtocolCountersReadModel {
        return protocolReadRepository.getCounters()
    }
}

@Service
class GetClientProtocolHistoryQueryHandler(
    private val protocolReadRepository: ProtocolReadRepository
) : QueryHandler<GetClientProtocolHistoryQuery, List<ProtocolListReadModel>> {

    override fun handle(query: GetClientProtocolHistoryQuery): List<ProtocolListReadModel> {
        return protocolReadRepository.findByClientId(query.clientId)
    }
}