package com.carslab.crm.production.modules.visits.domain.policy

import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class VisitBusinessPolicy {

    fun validateStatusTransition(currentStatus: VisitStatus, newStatus: VisitStatus) {
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw BusinessException("Cannot transition from $currentStatus to $newStatus")
        }
    }

    fun validateDeletion(visit: Visit) {
        if (visit.status == VisitStatus.IN_PROGRESS) {
            throw BusinessException("Cannot delete visit that is in progress")
        }

        if (visit.status == VisitStatus.COMPLETED) {
            throw BusinessException("Cannot delete completed visit")
        }
    }

    fun canModifyServices(visit: Visit): Boolean {
        return visit.status != VisitStatus.COMPLETED
    }
    
    fun canReleaseVehicle(visit: Visit): Boolean {
        return visit.status == VisitStatus.READY_FOR_PICKUP
    }

    fun validateReleaseConditions(visit: Visit) {
        if (!canReleaseVehicle(visit)) {
            throw BusinessException("Cannot release vehicle for visit with status: ${visit.status}")
        }

        if (visit.services.isEmpty()) {
            throw BusinessException("Cannot release vehicle - no services provided")
        }
    }
}