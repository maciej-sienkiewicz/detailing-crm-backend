package com.carslab.crm.production.modules.events.infrastructure.entity

import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDateTime

@Entity
@Table(
    name = "recurring_events",
    indexes = [
        Index(name = "idx_recurring_events_company_id", columnList = "companyId"),
        Index(name = "idx_recurring_events_active", columnList = "companyId,isActive"),
        Index(name = "idx_recurring_events_type", columnList = "companyId,eventType")
    ]
)
class RecurringEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val eventType: EventType,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val frequency: RecurrenceFrequency,

    @Column(nullable = false)
    val intervalValue: Int = 1,

    @Column(columnDefinition = "TEXT")
    val daysOfWeek: String? = null,

    val dayOfMonth: Int? = null,

    val endDate: LocalDateTime? = null,

    val maxOccurrences: Int? = null,

    @Column(columnDefinition = "TEXT")
    val visitTemplateJson: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(objectMapper: ObjectMapper): RecurringEvent {
        val recurrencePattern = RecurrencePattern(
            frequency = frequency,
            interval = intervalValue,
            daysOfWeek = parseDaysOfWeek(daysOfWeek),
            dayOfMonth = dayOfMonth,
            endDate = endDate,
            maxOccurrences = maxOccurrences
        )

        val visitTemplate = visitTemplateJson?.let { json ->
            try {
                objectMapper.readValue(json, VisitTemplate::class.java)
            } catch (e: Exception) {
                null
            }
        }

        return RecurringEvent(
            id = id?.let { RecurringEventId.of(it) },
            companyId = companyId,
            title = title,
            description = description,
            type = eventType,
            recurrencePattern = recurrencePattern,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            visitTemplate = visitTemplate
        )
    }

    private fun parseDaysOfWeek(daysJson: String?): List<DayOfWeek>? {
        return if (daysJson.isNullOrBlank()) {
            null
        } else {
            try {
                val objectMapper = ObjectMapper()
                val dayNames: List<String> = objectMapper.readValue(daysJson, object : TypeReference<List<String>>() {})
                dayNames.map { DayOfWeek.valueOf(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        fun fromDomain(recurringEvent: RecurringEvent, objectMapper: ObjectMapper): RecurringEventEntity {
            val daysOfWeekJson = recurringEvent.recurrencePattern.daysOfWeek?.let { days ->
                try {
                    objectMapper.writeValueAsString(days.map { it.name })
                } catch (e: Exception) {
                    null
                }
            }

            val visitTemplateJson = recurringEvent.visitTemplate?.let { template ->
                try {
                    objectMapper.writeValueAsString(template)
                } catch (e: Exception) {
                    null
                }
            }

            return RecurringEventEntity(
                id = recurringEvent.id?.value,
                companyId = recurringEvent.companyId,
                title = recurringEvent.title,
                description = recurringEvent.description,
                eventType = recurringEvent.type,
                isActive = recurringEvent.isActive,
                frequency = recurringEvent.recurrencePattern.frequency,
                intervalValue = recurringEvent.recurrencePattern.interval,
                daysOfWeek = daysOfWeekJson,
                dayOfMonth = recurringEvent.recurrencePattern.dayOfMonth,
                endDate = recurringEvent.recurrencePattern.endDate,
                maxOccurrences = recurringEvent.recurrencePattern.maxOccurrences,
                visitTemplateJson = visitTemplateJson,
                createdAt = recurringEvent.createdAt,
                updatedAt = recurringEvent.updatedAt
            )
        }
    }
}