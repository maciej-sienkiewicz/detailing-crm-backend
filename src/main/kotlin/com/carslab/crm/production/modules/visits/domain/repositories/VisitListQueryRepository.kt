package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.modules.visits.application.queries.models.VisitListReadModel
import org.springframework.data.domain.Pageable

interface VisitListQueryRepository {
    fun findVisitList(companyId: Long, pageable: Pageable): PaginatedResponse<VisitListReadModel>
}