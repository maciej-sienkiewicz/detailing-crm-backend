package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.visits.application.dto.ChangeStatusRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.domain.command.ChangeVisitStatusCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import org.springframework.stereotype.Component

@Component
class VisitStatusChangeHandler(
    private val visitDomainService: VisitDomainService
) {

    fun handle(visitId: VisitId, request: ChangeStatusRequest, companyId: Long): VisitResponse {
        val command = ChangeVisitStatusCommand(
            visitId = visitId,
            newStatus = request.status,
            reason = request.reason,
            companyId = companyId
        )

        val visit = visitDomainService.changeVisitStatus(command)

        return VisitResponse.from(visit)
    }
}