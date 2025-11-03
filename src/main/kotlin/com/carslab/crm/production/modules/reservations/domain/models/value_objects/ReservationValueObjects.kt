package com.carslab.crm.production.modules.reservations.domain.models.value_objects

import java.time.LocalDateTime

@JvmInline
value class ReservationId(val value: Long) {
    init {
        require(value > 0) { "Reservation ID must be positive" }
    }

    companion object {
        fun of(value: Long): ReservationId = ReservationId(value)
        fun of(value: String): ReservationId = ReservationId(value.toLong())
    }

    override fun toString(): String = value.toString()
}

data class ReservationPeriod(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) {
    init {
        require(!startDate.isAfter(endDate)) { "Start date cannot be after end date" }
    }

    fun duration(): java.time.Duration = java.time.Duration.between(startDate, endDate)
    fun isActive(): Boolean = LocalDateTime.now().let { now ->
        now.isAfter(startDate) && now.isBefore(endDate)
    }
}

/**
 * Minimalne informacje o pojeździe - bez tablicy rejestracyjnej!
 */
data class VehicleBasicInfo(
    val make: String,
    val model: String
) {
    init {
        require(make.isNotBlank()) { "Vehicle make cannot be blank" }
        require(model.isNotBlank()) { "Vehicle model cannot be blank" }
    }

    fun displayName(): String = "$make $model"
}