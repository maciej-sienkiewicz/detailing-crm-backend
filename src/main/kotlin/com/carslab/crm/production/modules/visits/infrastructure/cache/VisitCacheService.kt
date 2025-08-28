package com.carslab.crm.production.modules.visits.infrastructure.cache

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import org.springframework.stereotype.Service

@Service
class VisitCacheService {

    fun getCachedVisitCount(companyId: Long, status: VisitStatus, countProvider: () -> Long): Long {
        return countProvider()
    }

    fun getCachedVisitCounters(companyId: Long, countersProvider: () -> VisitCountersCache): VisitCountersCache {
        return countersProvider()
    }

    fun evictVisitCaches(companyId: Long, visitId: Long?) {
        // No-op for now, implement when needed
    }

    fun evictAllVisitCounts() {
        // No-op for now, implement when needed
    }

    fun getCachedVisitDetail(visitId: Long, detailProvider: () -> Any?): Any? {
        return detailProvider()
    }

    data class VisitCountersCache(
        val scheduled: Long,
        val inProgress: Long,
        val readyForPickup: Long,
        val completed: Long,
        val cancelled: Long,
        val all: Long
    )
}