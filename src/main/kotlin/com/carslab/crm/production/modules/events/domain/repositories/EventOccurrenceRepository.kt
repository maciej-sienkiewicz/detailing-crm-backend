package com.carslab.crm.production.modules.events.domain.repositories

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface EventOccurrenceRepository {
    fun save(eventOccurrence: EventOccurrence): EventOccurrence
    fun saveAll(eventOccurrences: List<EventOccurrence>): List<EventOccurrence>
    fun findById(id: EventOccurrenceId): EventOccurrence?
    fun findByRecurringEventId(recurringEventId: RecurringEventId, pageable: Pageable): Page<EventOccurrence>
    fun findByRecurringEventIdAndDateRange(
        recurringEventId: RecurringEventId,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventOccurrence>
    fun findByCompanyIdAndDateRange(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<EventOccurrence>
    fun findByStatus(status: OccurrenceStatus, pageable: Pageable): Page<EventOccurrence>
    fun findByVisitId(visitId: VisitId): EventOccurrence?
    fun deleteById(id: EventOccurrenceId): Boolean
    fun deleteByRecurringEventId(recurringEventId: RecurringEventId): Int
    fun countByRecurringEventId(recurringEventId: RecurringEventId): Long
    fun countByRecurringEventIdAndStatus(recurringEventId: RecurringEventId, status: OccurrenceStatus): Long
    fun existsByRecurringEventIdAndScheduledDate(
        recurringEventId: RecurringEventId,
        scheduledDate: LocalDateTime
    ): Boolean
}