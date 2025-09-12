package com.carslab.crm.production.modules.events.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.EventOccurrenceResponse
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class EventCalendarQueryService(
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(EventCalendarQueryService::class.java)

    fun getEventCalendar(
        startDate: LocalDate,
        endDate: LocalDate,
        pageSize: Int = 1000
    ): List<EventOccurrenceResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching event calendar for company: {} from {} to {}", companyId, startDate, endDate)

        val pageable = PageRequest.of(0, pageSize)
        val occurrences = eventOccurrenceRepository.findByCompanyIdAndDateRange(
            companyId,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59),
            pageable
        )

        return occurrences.content.map { EventOccurrenceResponse.from(it) }
    }

    fun getUpcomingEvents(days: Int = 7): List<EventOccurrenceResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val fromDate = LocalDate.now()
        val toDate = fromDate.plusDays(days.toLong())

        logger.debug("Fetching upcoming events for company: {} for next {} days", companyId, days)

        val pageable = PageRequest.of(0, 100)
        val occurrences = eventOccurrenceRepository.findByCompanyIdAndDateRange(
            companyId,
            fromDate.atStartOfDay(),
            toDate.atTime(23, 59, 59),
            pageable
        )

        return occurrences.content
            .filter { it.status == OccurrenceStatus.PLANNED }
            .map { EventOccurrenceResponse.from(it) }
    }
}