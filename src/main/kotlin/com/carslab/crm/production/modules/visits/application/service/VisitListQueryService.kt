package com.carslab.crm.production.modules.visits.application.service

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListQueryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitListQueryService(
    private val visitListQueryRepository: VisitListQueryRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitListQueryService::class.java)

    fun getVisitList(pageable: Pageable): PaginatedResponse<VisitListReadModel> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit list for company: {}", companyId)

        val result = visitListQueryRepository.findVisitList(companyId, pageable)
        logger.debug("Found {} visits for company: {}", result.totalItems, companyId)

        return result
    }
}