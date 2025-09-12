package com.carslab.crm.production.modules.events.domain.services

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class RecurrenceCalculationService {

    fun generateOccurrences(
        event: RecurringEvent,
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        maxGenerate: Int = 1000
    ): List<EventOccurrence> {
        if (!event.isActive) return emptyList()

        val occurrences = mutableListOf<EventOccurrence>()
        var currentDate = fromDate
        var count = 0

        while (currentDate <= toDate && count < maxGenerate && shouldContinue(event.recurrencePattern, count, currentDate)) {
            val adjustedDate = adjustDateForPattern(currentDate, event.recurrencePattern)

            if (adjustedDate != null && adjustedDate <= toDate) {
                val occurrence = EventOccurrence(
                    id = null,
                    recurringEventId = event.id!!,
                    scheduledDate = adjustedDate,
                    status = OccurrenceStatus.PLANNED,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                occurrences.add(occurrence)
            }

            currentDate = calculateNextDate(currentDate, event.recurrencePattern) ?: break
            count++
        }

        return occurrences
    }

    fun calculateNextOccurrence(event: RecurringEvent, fromDate: LocalDateTime): EventOccurrence? {
        if (!event.isActive) return null

        val nextDate = calculateNextDate(fromDate, event.recurrencePattern)
            ?: return null

        val adjustedDate = adjustDateForPattern(nextDate, event.recurrencePattern)
            ?: return null

        return EventOccurrence(
            id = null,
            recurringEventId = event.id!!,
            scheduledDate = adjustedDate,
            status = OccurrenceStatus.PLANNED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun shouldContinue(pattern: RecurrencePattern, count: Int, currentDate: LocalDateTime): Boolean {
        return when {
            pattern.maxOccurrences != null && count >= pattern.maxOccurrences -> false
            pattern.endDate != null && currentDate.isAfter(pattern.endDate) -> false
            else -> true
        }
    }

    private fun calculateNextDate(currentDate: LocalDateTime, pattern: RecurrencePattern): LocalDateTime? {
        return when (pattern.frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(pattern.interval.toLong())
            RecurrenceFrequency.WEEKLY -> calculateNextWeeklyDate(currentDate, pattern)
            RecurrenceFrequency.MONTHLY -> calculateNextMonthlyDate(currentDate, pattern)
            RecurrenceFrequency.YEARLY -> currentDate.plusYears(pattern.interval.toLong())
        }
    }

    private fun calculateNextWeeklyDate(currentDate: LocalDateTime, pattern: RecurrencePattern): LocalDateTime {
        if (pattern.daysOfWeek.isNullOrEmpty()) {
            return currentDate.plusWeeks(pattern.interval.toLong())
        }

        val sortedDays = pattern.daysOfWeek.sortedBy { it.value }
        val currentDayOfWeek = currentDate.dayOfWeek

        val nextDayInWeek = sortedDays.find { it.value > currentDayOfWeek.value }

        return if (nextDayInWeek != null) {
            currentDate.with(TemporalAdjusters.next(nextDayInWeek))
        } else {
            currentDate.plusWeeks(pattern.interval.toLong()).with(TemporalAdjusters.nextOrSame(sortedDays.first()))
        }
    }

    private fun calculateNextMonthlyDate(currentDate: LocalDateTime, pattern: RecurrencePattern): LocalDateTime {
        var nextDate = currentDate.plusMonths(pattern.interval.toLong())

        pattern.dayOfMonth?.let { day ->
            nextDate = try {
                nextDate.withDayOfMonth(day)
            } catch (e: Exception) {
                nextDate.with(TemporalAdjusters.lastDayOfMonth())
            }
        }

        return nextDate
    }

    private fun adjustDateForPattern(date: LocalDateTime, pattern: RecurrencePattern): LocalDateTime? {
        return when (pattern.frequency) {
            RecurrenceFrequency.WEEKLY -> {
                if (pattern.daysOfWeek.isNullOrEmpty() || pattern.daysOfWeek.contains(date.dayOfWeek)) {
                    date
                } else {
                    null
                }
            }
            RecurrenceFrequency.MONTHLY -> {
                pattern.dayOfMonth?.let { day ->
                    try {
                        date.withDayOfMonth(day)
                    } catch (e: Exception) {
                        date.with(TemporalAdjusters.lastDayOfMonth())
                    }
                } ?: date
            }
            else -> date
        }
    }
}