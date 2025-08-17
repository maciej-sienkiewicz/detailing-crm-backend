package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.VisitCountersResponse
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitCountersQueryService(
    private val visitDomainService: VisitDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCountersQueryService::class.java)

    fun getVisitCounters(): VisitCountersResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit counters for company: {}", companyId)

        return VisitCountersResponse(
            scheduled = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.SCHEDULED),
            inProgress = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.IN_PROGRESS),
            readyForPickup = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.READY_FOR_PICKUP),
            completed = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.COMPLETED),
            cancelled = visitDomainService.getVisitCountByStatus(companyId, VisitStatus.CANCELLED),
            all = VisitStatus.values().sumOf { visitDomainService.getVisitCountByStatus(companyId, it) }
        )
    }
}