package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.application.queries.models.VisitDetailReadModel
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface VisitDetailQueryRepository {
    fun findVisitDetailById(visitId: VisitId, companyId: Long): VisitDetailReadModel?
}