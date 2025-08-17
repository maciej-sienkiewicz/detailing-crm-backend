package com.carslab.crm.production.modules.visits.domain.models.value_objects

import java.time.LocalDateTime

data class VisitPeriod(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) {
    init {
        require(!startDate.isAfter(endDate)) { "Start date cannot be after end date" }
    }

    fun duration(): java.time.Duration = java.time.Duration.between(startDate, endDate)
    fun isActive(): Boolean = LocalDateTime.now().let { now -> now.isAfter(startDate) && now.isBefore(endDate) }
    fun hasStarted(): Boolean = LocalDateTime.now().isAfter(startDate)
    fun hasEnded(): Boolean = LocalDateTime.now().isAfter(endDate)
}