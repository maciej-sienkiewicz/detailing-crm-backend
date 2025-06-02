package com.carslab.crm.clients.domain.model

import java.time.LocalDateTime

data class ClientVehicleAssociation(
    val clientId: ClientId,
    val vehicleId: VehicleId,
    val relationshipType: VehicleRelationshipType = VehicleRelationshipType.OWNER,
    val startDate: LocalDateTime = LocalDateTime.now(),
    val endDate: LocalDateTime? = null,
    val isPrimary: Boolean = false
)

enum class VehicleRelationshipType {
    OWNER, USER, AUTHORIZED_PERSON
}