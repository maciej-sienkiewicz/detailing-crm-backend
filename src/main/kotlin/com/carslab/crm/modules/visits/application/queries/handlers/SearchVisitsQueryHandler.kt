package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.infrastructure.persistence.read.VisitSearchRepositoryDeprecated
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.stereotype.Service

@Service
class SearchVisitsQueryHandler(
    private val visitSearchRepositoryDeprecated: VisitSearchRepositoryDeprecated
) : QueryHandler<SearchVisitsQuery, PaginatedResponse<VisitListReadModel>> {

    override fun handle(query: SearchVisitsQuery): PaginatedResponse<VisitListReadModel> {
        return visitSearchRepositoryDeprecated.searchVisits(query)
    }
}