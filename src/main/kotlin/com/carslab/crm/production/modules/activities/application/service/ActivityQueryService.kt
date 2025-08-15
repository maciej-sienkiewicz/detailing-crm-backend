package com.carslab.crm.production.modules.activities.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.ActivityPageResponse
import com.carslab.crm.production.modules.activities.application.dto.ActivityResponse
import com.carslab.crm.production.modules.activities.domain.service.ActivityDomainService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ActivityQueryService(
    private val domainService: ActivityDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ActivityQueryService::class.java)

    fun getActivitiesForCurrentCompany(page: Int = 0, size: Int = 50): ActivityPageResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching activities for company: {} (page: {}, size: {})", companyId, page, size)

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        val activitiesPage = domainService.getActivitiesForCompany(companyId, pageable)

        if (activitiesPage.isEmpty) {
            logger.debug("No activities found for company: {}", companyId)
            return ActivityPageResponse.from(Page.empty())
        }

        val activityIds = activitiesPage.content.map { it.id }
        val allRelations = domainService.getRelatedEntitiesForActivities(activityIds)
        val relationsByActivityId = allRelations.groupBy { it.activityId }

        logger.debug("Found {} activities with {} total relations",
            activitiesPage.content.size, allRelations.size)

        val responsePage = activitiesPage.map { activity ->
            val relatedEntities = relationsByActivityId[activity.id] ?: emptyList()
            ActivityResponse.from(activity, relatedEntities)
        }

        return ActivityPageResponse.from(responsePage)
    }

    fun getActivity(activityId: String): ActivityResponse? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching activity: {} for company: {}", activityId, companyId)

        val activity = domainService.getActivityForCompany(activityId, companyId)
        return activity?.let {
            val relatedEntities = domainService.getRelatedEntitiesForActivity(it.id)
            ActivityResponse.from(it, relatedEntities)
        }
    }
}
