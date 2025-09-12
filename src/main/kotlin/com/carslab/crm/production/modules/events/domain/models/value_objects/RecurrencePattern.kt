package com.carslab.crm.production.modules.events.domain.models.value_objects

import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.LocalDateTime

data class RecurrencePattern(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: List<DayOfWeek>? = null,
    val dayOfMonth: Int? = null,
    val endDate: LocalDateTime? = null,
    val maxOccurrences: Int? = null
) {
    init {
        require(interval > 0) { "Interval must be positive" }
        dayOfMonth?.let { require(it in 1..31) { "Day of month must be between 1 and 31" } }
        maxOccurrences?.let { require(it > 0) { "Max occurrences must be positive" } }
    }

    fun calculateNextDate(fromDate: LocalDateTime): LocalDateTime? {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> fromDate.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> fromDate.plusWeeks(interval.toLong())
            RecurrenceFrequency.MONTHLY -> fromDate.plusMonths(interval.toLong())
            RecurrenceFrequency.YEARLY -> fromDate.plusYears(interval.toLong())
        }
    }

    fun hasEndCondition(): Boolean = endDate != null || maxOccurrences != null
}