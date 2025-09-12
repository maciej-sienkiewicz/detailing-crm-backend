package com.carslab.crm.production.modules.events.domain.models.enums

enum class OccurrenceStatus {
    PLANNED,
    COMPLETED,
    CONVERTED_TO_VISIT,
    SKIPPED,
    CANCELLED;

    fun canTransitionTo(newStatus: OccurrenceStatus): Boolean {
        return when (this) {
            PLANNED -> newStatus in listOf(COMPLETED, CONVERTED_TO_VISIT, SKIPPED, CANCELLED)
            COMPLETED -> false
            CONVERTED_TO_VISIT -> false
            SKIPPED -> newStatus in listOf(PLANNED, CANCELLED)
            CANCELLED -> false
        }
    }
}