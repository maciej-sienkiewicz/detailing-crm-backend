package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.application.queries.models.*
import org.springframework.data.domain.Pageable

interface VisitQueryRepository {
    fun findVisitList(companyId: Long, pageable: Pageable): PaginatedResponse<VisitListReadModel>
    fun findVisitDetail(visitId: VisitId, companyId: Long): VisitDetailReadModel?
    fun searchVisits(criteria: VisitSearchCriteria, pageable: Pageable): PaginatedResponse<VisitListReadModel>
    fun getVisitCounters(companyId: Long): VisitCountersReadModel
}