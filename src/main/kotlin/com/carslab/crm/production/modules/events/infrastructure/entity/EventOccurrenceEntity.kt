package com.carslab.crm.production.modules.events.infrastructure.entity

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_occurrences",
    indexes = [
        Index(name = "idx_occurrences_event_date", columnList = "recurringEventId,scheduledDate"),
        Index(name = "idx_occurrences_status", columnList = "status"),
        Index(name = "idx_occurrences_visit", columnList = "actualVisitId"),
        Index(name = "idx_occurrences_scheduled_date", columnList = "scheduledDate")
    ]
)
class EventOccurrenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val recurringEventId: Long,

    @Column(nullable = false)
    val scheduledDate: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: OccurrenceStatus,

    val actualVisitId: Long? = null,

    val completedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): EventOccurrence {
        return EventOccurrence(
            id = id?.let { EventOccurrenceId.of(it) },
            recurringEventId = RecurringEventId.of(recurringEventId),
            scheduledDate = scheduledDate,
            status = status,
            actualVisitId = actualVisitId?.let { VisitId.of(it) },
            completedAt = completedAt,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(eventOccurrence: EventOccurrence): EventOccurrenceEntity {
            return EventOccurrenceEntity(
                id = eventOccurrence.id?.value,
                recurringEventId = eventOccurrence.recurringEventId.value,
                scheduledDate = eventOccurrence.scheduledDate,
                status = eventOccurrence.status,
                actualVisitId = eventOccurrence.actualVisitId?.value,
                completedAt = eventOccurrence.completedAt,
                notes = eventOccurrence.notes,
                createdAt = eventOccurrence.createdAt,
                updatedAt = eventOccurrence.updatedAt
            )
        }
    }
}