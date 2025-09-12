package com.carslab.crm.production.modules.events.domain.models.value_objects

@JvmInline
value class EventOccurrenceId(val value: Long) {
    init {
        require(value > 0) { "EventOccurrence ID must be positive" }
    }

    companion object {
        fun of(value: Long): EventOccurrenceId = EventOccurrenceId(value)
        fun of(value: String): EventOccurrenceId = EventOccurrenceId(value.toLong())
    }

    override fun toString(): String = value.toString()
}