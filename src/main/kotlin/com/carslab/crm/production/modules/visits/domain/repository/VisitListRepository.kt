package com.carslab.crm.production.modules.visits.domain.repository

import com.carslab.crm.production.modules.visits.domain.model.VisitListItem
import com.carslab.crm.production.modules.visits.domain.model.VisitListService
import com.carslab.crm.production.modules.visits.domain.model.VisitId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VisitListRepository {
    fun findVisitListForCompany(companyId: Long, pageable: Pageable): Page<VisitListItem>
    fun findVisitServicesForVisits(companyId: Long, visitIds: List<VisitId>): Map<VisitId, List<VisitListService>>
}