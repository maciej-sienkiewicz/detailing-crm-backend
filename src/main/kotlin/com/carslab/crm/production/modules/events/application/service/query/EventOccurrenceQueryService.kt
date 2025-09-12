package com.carslab.crm.production.modules.events.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.EventOccurrenceResponse
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class EventOccurrenceQueryService(
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(EventOccurrenceQueryService::class.java)

    fun getOccurrences(
        recurringEventId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<EventOccurrenceResponse> {
        logger.debug("Fetching occurrences for event: {} from {} to {}", recurringEventId, startDate, endDate)

        val occurrences = eventOccurrenceRepository.findByRecurringEventIdAndDateRange(
            RecurringEventId.of(recurringEventId),
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        )

        return occurrences.map { EventOccurrenceResponse.from(it) }
    }

    fun getOccurrencesByEvent(recurringEventId: Long, pageable: Pageable): Page<EventOccurrenceResponse> {
        logger.debug("Fetching all occurrences for event: {}", recurringEventId)

        val occurrences = eventOccurrenceRepository.findByRecurringEventId(
            RecurringEventId.of(recurringEventId),
            pageable
        )
        return occurrences.map { EventOccurrenceResponse.from(it) }
    }

    fun getOccurrencesInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable
    ): Page<EventOccurrenceResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching occurrences for company: {} from {} to {}", companyId, startDate, endDate)

        val occurrences = eventOccurrenceRepository.findByCompanyIdAndDateRange(
            companyId,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59),
            pageable
        )

        return occurrences.map { EventOccurrenceResponse.from(it) }
    }

    fun getOccurrencesByStatus(status: String, pageable: Pageable): Page<EventOccurrenceResponse> {
        val occurrenceStatus = try {
            OccurrenceStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid occurrence status: $status")
        }

        logger.debug("Fetching occurrences with status: {}", status)

        val occurrences = eventOccurrenceRepository.findByStatus(occurrenceStatus, pageable)
        return occurrences.map { EventOccurrenceResponse.from(it) }
    }

    fun getOccurrenceStatistics(recurringEventId: Long): Map<String, Long> {
        val eventId = RecurringEventId.of(recurringEventId)

        return mapOf(
            "total" to eventOccurrenceRepository.countByRecurringEventId(eventId),
            "planned" to eventOccurrenceRepository.countByRecurringEventIdAndStatus(eventId, OccurrenceStatus.PLANNED),
            "completed" to eventOccurrenceRepository.countByRecurringEventIdAndStatus(eventId, OccurrenceStatus.COMPLETED),
            "converted_to_visit" to eventOccurrenceRepository.countByRecurringEventIdAndStatus(eventId, OccurrenceStatus.CONVERTED_TO_VISIT),
            "cancelled" to eventOccurrenceRepository.countByRecurringEventIdAndStatus(eventId, OccurrenceStatus.CANCELLED)
        )
    }
}