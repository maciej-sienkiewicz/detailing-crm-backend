package com.carslab.crm.production.modules.events.domain.models.aggregates

import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import java.time.LocalDateTime

data class EventOccurrence(
    val id: EventOccurrenceId?,
    val recurringEventId: RecurringEventId,
    val scheduledDate: LocalDateTime,
    val status: OccurrenceStatus,
    val actualVisitId: VisitId? = null,
    val completedAt: LocalDateTime? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    init {
        require(scheduledDate.isAfter(LocalDateTime.now().minusYears(1))) {
            "Scheduled date cannot be too far in the past"
        }
        if (status == OccurrenceStatus.COMPLETED) {
            require(completedAt != null) { "Completed at date is required for completed occurrences" }
        }
        if (status == OccurrenceStatus.CONVERTED_TO_VISIT) {
            require(actualVisitId != null) { "Visit ID is required for converted occurrences" }
        }
    }

    fun markAsCompleted(): EventOccurrence {
        require(status.canTransitionTo(OccurrenceStatus.COMPLETED)) {
            "Cannot transition from $status to COMPLETED"
        }
        return copy(
            status = OccurrenceStatus.COMPLETED,
            completedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun convertToVisit(visitId: VisitId): EventOccurrence {
        require(status.canTransitionTo(OccurrenceStatus.CONVERTED_TO_VISIT)) {
            "Cannot transition from $status to CONVERTED_TO_VISIT"
        }
        return copy(
            status = OccurrenceStatus.CONVERTED_TO_VISIT,
            actualVisitId = visitId,
            updatedAt = LocalDateTime.now()
        )
    }

    fun changeStatus(newStatus: OccurrenceStatus): EventOccurrence {
        require(status.canTransitionTo(newStatus)) {
            "Cannot transition from $status to $newStatus"
        }
        return copy(status = newStatus, updatedAt = LocalDateTime.now())
    }

    fun addNotes(additionalNotes: String): EventOccurrence {
        val updatedNotes = if (notes.isNullOrBlank()) {
            additionalNotes
        } else {
            "$notes\n$additionalNotes"
        }
        return copy(notes = updatedNotes, updatedAt = LocalDateTime.now())
    }

    fun isPlanned(): Boolean = status == OccurrenceStatus.PLANNED
    fun isCompleted(): Boolean = status == OccurrenceStatus.COMPLETED
    fun isConvertedToVisit(): Boolean = status == OccurrenceStatus.CONVERTED_TO_VISIT
    fun isCancelled(): Boolean = status == OccurrenceStatus.CANCELLED
}