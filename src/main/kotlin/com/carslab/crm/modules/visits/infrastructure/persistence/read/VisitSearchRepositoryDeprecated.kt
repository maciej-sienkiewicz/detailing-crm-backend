package com.carslab.crm.modules.visits.infrastructure.persistence.read

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.api.model.response.PaginatedResponse

interface VisitSearchRepositoryDeprecated {
    fun searchVisits(query: SearchVisitsQuery): PaginatedResponse<VisitListReadModel>
}