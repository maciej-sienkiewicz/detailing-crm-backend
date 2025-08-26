package com.carslab.crm.production.modules.visits.domain.service.policy

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

    fun canAddMedia(visit: Visit): Boolean {
        return visit.status != VisitStatus.CANCELLED
    }

    fun canAddComments(visit: Visit): Boolean {
        return true
    }

    fun canReleaseVehicle(visit: Visit): Boolean {
        return visit.status == VisitStatus.READY_FOR_PICKUP
    }
}