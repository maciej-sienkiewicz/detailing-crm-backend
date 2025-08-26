package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.VisitCountersResponse
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.infrastructure.cache.VisitCacheService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitCountersQueryService(
    private val visitDomainService: VisitDomainService,
    private val cacheService: VisitCacheService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitCountersQueryService::class.java)

    fun getVisitCounters(): VisitCountersResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visit counters for company: {}", companyId)

        val cachedCounters = cacheService.getCachedVisitCounters(companyId) {
            calculateCounters(companyId)
        }

        return VisitCountersResponse(
            scheduled = cachedCounters.scheduled,
            inProgress = cachedCounters.inProgress,
            readyForPickup = cachedCounters.readyForPickup,
            completed = cachedCounters.completed,
            cancelled = cachedCounters.cancelled,
            all = cachedCounters.all
        )
    }

    private fun calculateCounters(companyId: Long): VisitCacheService.VisitCountersCache {
        val scheduled = getCountForStatus(companyId, VisitStatus.SCHEDULED)
        val inProgress = getCountForStatus(companyId, VisitStatus.IN_PROGRESS)
        val readyForPickup = getCountForStatus(companyId, VisitStatus.READY_FOR_PICKUP)
        val completed = getCountForStatus(companyId, VisitStatus.COMPLETED)
        val cancelled = getCountForStatus(companyId, VisitStatus.CANCELLED)

        return VisitCacheService.VisitCountersCache(
            scheduled = scheduled,
            inProgress = inProgress,
            readyForPickup = readyForPickup,
            completed = completed,
            cancelled = cancelled,
            all = scheduled + inProgress + readyForPickup + completed + cancelled
        )
    }

    private fun getCountForStatus(companyId: Long, status: VisitStatus): Long {
        return cacheService.getCachedVisitCount(companyId, status) {
            visitDomainService.getVisitCountByStatus(companyId, status)
        }
    }
}