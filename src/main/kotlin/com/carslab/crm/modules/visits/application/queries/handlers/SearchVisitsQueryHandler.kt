package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.infrastructure.persistence.read.VisitSearchRepository
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.stereotype.Service

@Service
class SearchVisitsQueryHandler(
    private val visitSearchRepository: VisitSearchRepository
) : QueryHandler<SearchVisitsQuery, PaginatedResponse<VisitListReadModel>> {

    override fun handle(query: SearchVisitsQuery): PaginatedResponse<VisitListReadModel> {
        return visitSearchRepository.searchVisits(query)
    }
}