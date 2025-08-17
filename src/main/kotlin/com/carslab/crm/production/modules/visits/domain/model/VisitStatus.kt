package com.carslab.crm.production.modules.visits.domain.model

enum class VisitStatus {
    SCHEDULED,
    PENDING_APPROVAL,
    IN_PROGRESS,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED;

    fun canTransitionTo(newStatus: VisitStatus): Boolean {
        return when (this) {
            SCHEDULED -> newStatus in listOf(IN_PROGRESS, CANCELLED, PENDING_APPROVAL)
            PENDING_APPROVAL -> newStatus in listOf(SCHEDULED, IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> newStatus in listOf(READY_FOR_PICKUP, CANCELLED)
            READY_FOR_PICKUP -> newStatus in listOf(COMPLETED, IN_PROGRESS)
            COMPLETED -> false
            CANCELLED -> false
        }
    }
}