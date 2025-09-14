package com.carslab.crm.production.modules.events.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.EventOccurrenceResponse
import com.carslab.crm.production.modules.events.application.dto.EventOccurrenceWithDetailsResponse
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class EventCalendarQueryService(
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val recurringEventRepository: RecurringEventRepository, // DODANE
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

    /**
     * NEW: Optimized method to fetch calendar events with embedded recurring event details
     * This eliminates N+1 queries by fetching all related data in minimal database calls
     */
    fun getEventCalendarWithDetails(
        startDate: LocalDate,
        endDate: LocalDate,
        includeEventDetails: Boolean = true
    ): List<EventOccurrenceWithDetailsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Fetching event calendar with details for company: {} from {} to {} (includeDetails: {})",
            companyId, startDate, endDate, includeEventDetails)

        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        // Step 1: Get all occurrences in the date range (single query)
        val occurrences = eventOccurrenceRepository.findByCompanyIdAndDateRangeWithoutPagination(
            companyId,
            startDateTime,
            endDateTime
        )

        if (!includeEventDetails || occurrences.isEmpty()) {
            logger.info("Returning {} occurrences without event details", occurrences.size)
            return occurrences.map { EventOccurrenceWithDetailsResponse.fromOccurrenceOnly(it) }
        }

        // Step 2: Get all unique recurring event IDs
        val recurringEventIds = occurrences.map { it.recurringEventId }.distinct()

        // Step 3: Fetch all recurring events in a single query (avoids N+1)
        val recurringEvents = recurringEventRepository.findAllByIds(
            recurringEventIds.map { it.value }
        ).associateBy { it.id }

        // Step 4: Combine occurrences with their corresponding events
        val result = occurrences.map { occurrence ->
            val recurringEvent = recurringEvents[occurrence.recurringEventId]
            EventOccurrenceWithDetailsResponse.from(occurrence, recurringEvent)
        }

        logger.info("Retrieved {} occurrences with details ({} unique events)", result.size, recurringEvents.size)

        return result
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