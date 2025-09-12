package com.carslab.crm.production.modules.events.domain.services

import com.carslab.crm.production.modules.events.domain.models.enums.RecurrenceFrequency
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurrencePattern
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventValidationService {

    fun validateRecurrencePattern(pattern: RecurrencePattern) {
        validateInterval(pattern.interval)
        validateDaysOfWeek(pattern)
        validateDayOfMonth(pattern)
        validateEndConditions(pattern)
    }

    fun validateEventDates(startDate: LocalDateTime, endDate: LocalDateTime?) {
        endDate?.let {
            if (startDate.isAfter(endDate)) {
                throw BusinessException("Start date cannot be after end date")
            }
        }
    }

    private fun validateInterval(interval: Int) {
        if (interval <= 0) {
            throw BusinessException("Recurrence interval must be positive")
        }
        if (interval > 365) {
            throw BusinessException("Recurrence interval cannot exceed 365")
        }
    }

    private fun validateDaysOfWeek(pattern: RecurrencePattern) {
        if (pattern.frequency == RecurrenceFrequency.WEEKLY) {
            pattern.daysOfWeek?.let { days ->
                if (days.isEmpty()) {
                    throw BusinessException("At least one day of week must be specified for weekly recurrence")
                }
                if (days.size > 7) {
                    throw BusinessException("Cannot specify more than 7 days of week")
                }
            }
        }
    }

    private fun validateDayOfMonth(pattern: RecurrencePattern) {
        if (pattern.frequency == RecurrenceFrequency.MONTHLY) {
            pattern.dayOfMonth?.let { day ->
                if (day !in 1..31) {
                    throw BusinessException("Day of month must be between 1 and 31")
                }
            }
        }
    }

    private fun validateEndConditions(pattern: RecurrencePattern) {
        pattern.endDate?.let { endDate ->
            if (endDate.isBefore(LocalDateTime.now())) {
                throw BusinessException("End date cannot be in the past")
            }
        }

        pattern.maxOccurrences?.let { max ->
            if (max <= 0) {
                throw BusinessException("Max occurrences must be positive")
            }
            if (max > 10000) {
                throw BusinessException("Max occurrences cannot exceed 10000")
            }
        }
    }
}