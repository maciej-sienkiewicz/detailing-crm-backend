package com.carslab.crm.production.modules.events.domain.models.aggregates

import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
import java.time.LocalDateTime

data class RecurringEvent(
    val id: RecurringEventId?,
    val companyId: Long,
    val title: String,
    val description: String?,
    val type: EventType,
    val recurrencePattern: RecurrencePattern,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val visitTemplate: VisitTemplate? = null
) {
    init {
        require(title.isNotBlank()) { "Event title cannot be blank" }
        require(companyId > 0) { "Company ID must be positive" }
        if (type == EventType.RECURRING_VISIT) {
            require(visitTemplate != null) { "Visit template is required for recurring visits" }
        }
    }

    fun deactivate(): RecurringEvent {
        return copy(isActive = false, updatedAt = LocalDateTime.now())
    }

    fun updateTitle(newTitle: String): RecurringEvent {
        require(newTitle.isNotBlank()) { "Event title cannot be blank" }
        return copy(title = newTitle, updatedAt = LocalDateTime.now())
    }

    fun updateDescription(newDescription: String?): RecurringEvent {
        return copy(description = newDescription, updatedAt = LocalDateTime.now())
    }

    fun updateRecurrencePattern(newPattern: RecurrencePattern): RecurringEvent {
        return copy(recurrencePattern = newPattern, updatedAt = LocalDateTime.now())
    }

    fun generateNextOccurrenceDate(fromDate: LocalDateTime): LocalDateTime? {
        if (!isActive) return null
        return recurrencePattern.calculateNextDate(fromDate)
    }

    fun isRecurringVisit(): Boolean = type == EventType.RECURRING_VISIT
    fun isSimpleEvent(): Boolean = type == EventType.SIMPLE_EVENT
}
