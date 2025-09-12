package com.carslab.crm.production.modules.events.domain.models.value_objects

@JvmInline
value class RecurringEventId(val value: Long) {
    init {
        require(value > 0) { "RecurringEvent ID must be positive" }
    }

    companion object {
        fun of(value: Long): RecurringEventId = RecurringEventId(value)
        fun of(value: String): RecurringEventId = RecurringEventId(value.toLong())
    }

    override fun toString(): String = value.toString()
}