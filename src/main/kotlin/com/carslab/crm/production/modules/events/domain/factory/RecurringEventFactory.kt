package com.carslab.crm.production.modules.events.domain.factory

import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
import com.carslab.crm.production.modules.events.domain.services.EventValidationService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecurringEventFactory(
    private val eventValidationService: EventValidationService
) {

    fun createSimpleEvent(
        companyId: Long,
        title: String,
        description: String?,
        recurrencePattern: RecurrencePattern
    ): RecurringEvent {
        eventValidationService.validateRecurrencePattern(recurrencePattern)

        return RecurringEvent(
            id = null,
            companyId = companyId,
            title = title.trim(),
            description = description?.trim(),
            type = EventType.SIMPLE_EVENT,
            recurrencePattern = recurrencePattern,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            visitTemplate = null
        )
    }

    fun createRecurringVisit(
        companyId: Long,
        title: String,
        description: String?,
        recurrencePattern: RecurrencePattern,
        visitTemplate: VisitTemplate
    ): RecurringEvent {
        eventValidationService.validateRecurrencePattern(recurrencePattern)

        return RecurringEvent(
            id = null,
            companyId = companyId,
            title = title.trim(),
            description = description?.trim(),
            type = EventType.RECURRING_VISIT,
            recurrencePattern = recurrencePattern,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            visitTemplate = visitTemplate
        )
    }

    fun updateEvent(
        existingEvent: RecurringEvent,
        title: String?,
        description: String?,
        recurrencePattern: RecurrencePattern?,
        visitTemplate: VisitTemplate?
    ): RecurringEvent {
        recurrencePattern?.let { eventValidationService.validateRecurrencePattern(it) }

        return existingEvent.copy(
            title = title?.trim() ?: existingEvent.title,
            description = description?.trim() ?: existingEvent.description,
            recurrencePattern = recurrencePattern ?: existingEvent.recurrencePattern,
            visitTemplate = visitTemplate ?: existingEvent.visitTemplate,
            updatedAt = LocalDateTime.now()
        )
    }
}
