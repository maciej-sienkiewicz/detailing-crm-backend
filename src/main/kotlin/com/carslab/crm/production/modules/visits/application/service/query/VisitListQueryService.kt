package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.VisitListFilterRequest
import com.carslab.crm.production.modules.visits.application.queries.models.VisitListReadModel
import com.carslab.crm.production.modules.visits.domain.repositories.VisitListQueryRepository
import com.carslab.crm.production.modules.visits.domain.service.VisitListCriteriaBuilder
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitListQueryService(
    private val visitListQueryRepository: VisitListQueryRepository,
    private val visitListCriteriaBuilder: VisitListCriteriaBuilder,
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

    fun getVisitList(filter: VisitListFilterRequest, pageable: Pageable): PaginatedResponse<VisitListReadModel> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching filtered visit list for company: {}", companyId)

        val criteria = visitListCriteriaBuilder.buildCriteria(companyId, filter)
        val result = visitListQueryRepository.findVisitList(criteria, pageable)
        logger.debug("Found {} filtered visits for company: {}", result.totalItems, companyId)

        return result
    }
}