package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListItem
import com.carslab.crm.production.modules.visits.domain.models.aggregates.VisitListService
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VisitListRepository {
    fun findVisitListForCompany(companyId: Long, pageable: Pageable): Page<VisitListItem>
    fun findVisitServicesForVisits(companyId: Long, visitIds: List<VisitId>): Map<VisitId, List<VisitListService>>
}