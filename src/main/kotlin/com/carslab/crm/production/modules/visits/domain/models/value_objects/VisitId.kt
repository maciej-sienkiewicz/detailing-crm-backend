package com.carslab.crm.production.modules.visits.domain.models.value_objects

@JvmInline
value class VisitId(val value: Long) {
    init {
        require(value > 0) { "Visit ID must be positive" }
    }

    companion object {
        fun of(value: Long): VisitId = VisitId(value)
        fun of(value: String): VisitId = VisitId(value.toLong())
    }

    override fun toString(): String = value.toString()
}