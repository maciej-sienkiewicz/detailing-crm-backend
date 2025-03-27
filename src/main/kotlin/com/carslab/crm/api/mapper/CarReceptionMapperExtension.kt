package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.domain.model.ProtocolStatus

object CarReceptionMapperExtension {
    /**
     * Maps API protocol status to domain protocol status.
     * This method is provided to ensure there's a direct mapping available.
     */
    fun mapStatus(apiStatus: ApiProtocolStatus): ProtocolStatus {
        return when (apiStatus) {
            ApiProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
            ApiProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
            ApiProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
            ApiProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
            ApiProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
        }
    }
}