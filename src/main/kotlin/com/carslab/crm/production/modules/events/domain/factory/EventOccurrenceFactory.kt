package com.carslab.crm.production.modules.events.domain.factory

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EventOccurrenceFactory {

    fun createOccurrence(
        recurringEventId: RecurringEventId,
        scheduledDate: LocalDateTime,
        notes: String? = null
    ): EventOccurrence {
        return EventOccurrence(
            id = null,
            recurringEventId = recurringEventId,
            scheduledDate = scheduledDate,
            status = OccurrenceStatus.PLANNED,
            actualVisitId = null,
            completedAt = null,
            notes = notes?.trim(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun createOccurrenceFromVisit(
        recurringEventId: RecurringEventId,
        scheduledDate: LocalDateTime,
        visitId: VisitId
    ): EventOccurrence {
        return EventOccurrence(
            id = null,
            recurringEventId = recurringEventId,
            scheduledDate = scheduledDate,
            status = OccurrenceStatus.CONVERTED_TO_VISIT,
            actualVisitId = visitId,
            completedAt = null,
            notes = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateOccurrence(
        existingOccurrence: EventOccurrence,
        status: OccurrenceStatus? = null,
        notes: String? = null,
        visitId: VisitId? = null
    ): EventOccurrence {
        return existingOccurrence.copy(
            status = status ?: existingOccurrence.status,
            notes = notes?.trim() ?: existingOccurrence.notes,
            actualVisitId = visitId ?: existingOccurrence.actualVisitId,
            completedAt = if (status == OccurrenceStatus.COMPLETED) LocalDateTime.now() else existingOccurrence.completedAt,
            updatedAt = LocalDateTime.now()
        )
    }
}