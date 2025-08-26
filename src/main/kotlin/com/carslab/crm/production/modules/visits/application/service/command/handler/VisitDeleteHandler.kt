package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import org.springframework.stereotype.Component

@Component
class VisitDeleteHandler(
    private val visitDomainService: VisitDomainService
) {

    fun handle(visitId: VisitId, companyId: Long): Boolean {
        return visitDomainService.deleteVisit(visitId, companyId)
    }
}