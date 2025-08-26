package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import org.springframework.stereotype.Component

@Component
class VisitReleaseHandler(
    private val visitDomainService: AggregateService
) {

    fun handle(visitId: String, request: ReleaseVehicleRequest, companyId: Long): Boolean {
        return visitDomainService.releaseVehicle(
            VisitId(visitId.toLong()),
            request,
            companyId
        )
    }
}