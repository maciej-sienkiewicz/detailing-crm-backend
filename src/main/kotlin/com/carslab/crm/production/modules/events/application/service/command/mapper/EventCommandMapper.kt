// src/main/kotlin/com/carslab/crm/production/modules/events/application/service/command/mapper/EventCommandMapper.kt
package com.carslab.crm.production.modules.events.application.service.command.mapper

import com.carslab.crm.production.modules.events.application.dto.RecurrencePatternRequest
import com.carslab.crm.production.modules.events.application.dto.ServiceTemplateRequest
import com.carslab.crm.production.modules.events.application.dto.VisitTemplateRequest
import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.modules.events.domain.models.value_objects.ServiceTemplate
import com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class EventCommandMapper {

    fun mapRecurrencePattern(request: RecurrencePatternRequest): RecurrencePattern {
        val frequency = try {
            RecurrenceFrequency.valueOf(request.frequency.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid frequency: ${request.frequency}")
        }

        val daysOfWeek = request.daysOfWeek?.let { days ->
            days.map { dayName ->
                try {
                    DayOfWeek.valueOf(dayName.convertShortcutToFullDayOfWeek())
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid day of week: $dayName")
                }
            }
        }

        val endDate = request.endDate?.let { dateStr ->
            try {
                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                try {
                    LocalDateTime.parse("${dateStr}T00:00:00")
                } catch (e2: Exception) {
                    throw IllegalArgumentException("Invalid end date format: $dateStr")
                }
            }
        }

        return RecurrencePattern(
            frequency = frequency,
            interval = request.interval,
            daysOfWeek = daysOfWeek,
            dayOfMonth = request.dayOfMonth,
            endDate = endDate,
            maxOccurrences = request.maxOccurrences
        )
    }
    
    private fun String.convertShortcutToFullDayOfWeek() = 
        when (this.lowercase()) {
            "mon" -> "MONDAY"
            "tue" -> "TUESDAY"
            "wed" -> "WEDNESDAY"
            "thu" -> "THURSDAY"
            "fri" -> "FRIDAY"
            "sat" -> "SATURDAY"
            "sun" -> "SUNDAY"
            else -> this.uppercase()
        }

    fun mapVisitTemplate(request: VisitTemplateRequest): VisitTemplate {
        val estimatedDuration = Duration.ofMinutes(request.estimatedDurationMinutes)
        val defaultServices = request.defaultServices.map { mapServiceTemplate(it) }

        return VisitTemplate(
            clientId = request.clientId,
            vehicleId = request.vehicleId,
            estimatedDuration = estimatedDuration,
            defaultServices = defaultServices,
            notes = request.notes
        )
    }

    fun mapServiceTemplate(request: ServiceTemplateRequest): ServiceTemplate {
        return ServiceTemplate(
            name = request.name,
            basePrice = request.basePrice
        )
    }
}