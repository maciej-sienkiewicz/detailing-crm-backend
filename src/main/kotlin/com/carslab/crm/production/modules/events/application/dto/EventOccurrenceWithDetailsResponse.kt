// src/main/kotlin/com/carslab/crm/production/modules/events/application/dto/EventOccurrenceWithDetailsResponse.kt
package com.carslab.crm.production.modules.events.application.dto

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for event occurrences with embedded recurring event details
 * This solves the N+1 query problem by including all necessary data in a single response
 */
data class EventOccurrenceWithDetailsResponse(
    val id: String,
    @JsonProperty("recurring_event_id")
    val recurringEventId: String,
    @JsonProperty("scheduled_date")
    val scheduledDate: LocalDateTime,
    val status: OccurrenceStatus,
    @JsonProperty("actual_visit_id")
    val actualVisitId: String?,
    @JsonProperty("completed_at")
    val completedAt: LocalDateTime?,
    val notes: String?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    // NEW: Embedded recurring event details
    @JsonProperty("recurring_event_details")
    val recurringEventDetails: RecurringEventResponse?
) {
    companion object {
        /**
         * Creates response from occurrence and recurring event entities
         */
        fun from(occurrence: EventOccurrence, recurringEvent: RecurringEvent?): EventOccurrenceWithDetailsResponse {
            return EventOccurrenceWithDetailsResponse(
                id = occurrence.id?.value?.toString() ?: "",
                recurringEventId = occurrence.recurringEventId.value.toString(),
                scheduledDate = occurrence.scheduledDate,
                status = occurrence.status,
                actualVisitId = occurrence.actualVisitId?.value?.toString(),
                completedAt = occurrence.completedAt,
                notes = occurrence.notes,
                createdAt = occurrence.createdAt,
                updatedAt = occurrence.updatedAt,
                recurringEventDetails = recurringEvent?.let { RecurringEventResponse.from(it) }
            )
        }

        /**
         * Creates response from occurrence only (without event details)
         */
        fun fromOccurrenceOnly(occurrence: EventOccurrence): EventOccurrenceWithDetailsResponse {
            return EventOccurrenceWithDetailsResponse(
                id = occurrence.id?.value?.toString() ?: "",
                recurringEventId = occurrence.recurringEventId.value.toString(),
                scheduledDate = occurrence.scheduledDate,
                status = occurrence.status,
                actualVisitId = occurrence.actualVisitId?.value?.toString(),
                completedAt = occurrence.completedAt,
                notes = occurrence.notes,
                createdAt = occurrence.createdAt,
                updatedAt = occurrence.updatedAt,
                recurringEventDetails = null
            )
        }
    }
}