package com.carslab.crm.production.modules.events.application.dto

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class EventOccurrenceResponse(
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
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(occurrence: EventOccurrence): EventOccurrenceResponse {
            return EventOccurrenceResponse(
                id = occurrence.id?.value?.toString() ?: "",
                recurringEventId = occurrence.recurringEventId.value.toString(),
                scheduledDate = occurrence.scheduledDate,
                status = occurrence.status,
                actualVisitId = occurrence.actualVisitId?.value?.toString(),
                completedAt = occurrence.completedAt,
                notes = occurrence.notes,
                createdAt = occurrence.createdAt,
                updatedAt = occurrence.updatedAt
            )
        }
    }
}