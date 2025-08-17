package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.application.queries.models.VisitListProjection
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitListService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VisitListRepository {
    fun findVisitListProjectionsForCompany(companyId: Long, pageable: Pageable): Page<VisitListProjection>
    fun findVisitServicesForVisits(companyId: Long, visitIds: List<VisitId>): Map<VisitId, List<VisitListService>>
}