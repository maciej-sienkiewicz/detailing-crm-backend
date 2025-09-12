package com.carslab.crm.production.modules.events.application.dto

import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.modules.events.domain.models.value_objects.ServiceTemplate
import com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class RecurringEventResponse(
    val id: String,
    val title: String,
    val description: String?,
    val type: EventType,
    @JsonProperty("recurrence_pattern")
    val recurrencePattern: RecurrencePatternResponse,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("visit_template")
    val visitTemplate: VisitTemplateResponse?,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(recurringEvent: RecurringEvent): RecurringEventResponse {
            return RecurringEventResponse(
                id = recurringEvent.id?.value?.toString() ?: "",
                title = recurringEvent.title,
                description = recurringEvent.description,
                type = recurringEvent.type,
                recurrencePattern = RecurrencePatternResponse.from(recurringEvent.recurrencePattern),
                isActive = recurringEvent.isActive,
                visitTemplate = recurringEvent.visitTemplate?.let { VisitTemplateResponse.from(it) },
                createdAt = recurringEvent.createdAt,
                updatedAt = recurringEvent.updatedAt
            )
        }
    }
}

data class RecurrencePatternResponse(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    @JsonProperty("days_of_week")
    val daysOfWeek: List<String>?,
    @JsonProperty("day_of_month")
    val dayOfMonth: Int?,
    @JsonProperty("end_date")
    val endDate: LocalDateTime?,
    @JsonProperty("max_occurrences")
    val maxOccurrences: Int?
) {
    companion object {
        fun from(pattern: RecurrencePattern): RecurrencePatternResponse {
            return RecurrencePatternResponse(
                frequency = pattern.frequency,
                interval = pattern.interval,
                daysOfWeek = pattern.daysOfWeek?.map { it.name },
                dayOfMonth = pattern.dayOfMonth,
                endDate = pattern.endDate,
                maxOccurrences = pattern.maxOccurrences
            )
        }
    }
}

data class VisitTemplateResponse(
    @JsonProperty("client_id")
    val clientId: Long?,
    @JsonProperty("vehicle_id")
    val vehicleId: Long?,
    @JsonProperty("estimated_duration_minutes")
    val estimatedDurationMinutes: Long,
    @JsonProperty("default_services")
    val defaultServices: List<ServiceTemplateResponse>,
    val notes: String?
) {
    companion object {
        fun from(template: VisitTemplate): VisitTemplateResponse {
            return VisitTemplateResponse(
                clientId = template.clientId,
                vehicleId = template.vehicleId,
                estimatedDurationMinutes = template.estimatedDuration.toMinutes(),
                defaultServices = template.defaultServices.map { ServiceTemplateResponse.from(it) },
                notes = template.notes
            )
        }
    }
}

data class ServiceTemplateResponse(
    val name: String,
    @JsonProperty("base_price")
    val basePrice: java.math.BigDecimal
) {
    companion object {
        fun from(template: ServiceTemplate): ServiceTemplateResponse {
            return ServiceTemplateResponse(
                name = template.name,
                basePrice = template.basePrice
            )
        }
    }
}