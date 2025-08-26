package com.carslab.crm.production.modules.visits.application.service.query.handler

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.visits.application.dto.VisitListFilterRequest
import com.carslab.crm.production.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.application.service.query.builder.VisitListCriteriaBuilder
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListQueryRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class VisitListQueryHandler(
    private val visitListQueryRepository: VisitListQueryRepository,
    private val criteriaBuilder: VisitListCriteriaBuilder
) {

    fun handleUnfiltered(companyId: Long, pageable: Pageable): PaginatedResponse<VisitListReadModel> {
        return visitListQueryRepository.findVisitList(companyId, pageable)
    }

    fun handleFiltered(
        companyId: Long,
        filter: VisitListFilterRequest,
        pageable: Pageable
    ): PaginatedResponse<VisitListReadModel> {
        val criteria = criteriaBuilder.buildCriteria(companyId, filter)
        return visitListQueryRepository.findVisitList(criteria, pageable)
    }
}