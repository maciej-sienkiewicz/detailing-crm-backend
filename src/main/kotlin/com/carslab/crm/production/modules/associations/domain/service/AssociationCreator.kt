package com.carslab.crm.production.modules.associations.domain.service

import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AssociationCreator {
    fun create(
        clientId: ClientId,
        vehicleId: VehicleId,
        companyId: Long,
        associationType: AssociationType = AssociationType.OWNER,
        isPrimary: Boolean = false
    ): ClientVehicleAssociation {
        return ClientVehicleAssociation(
            clientId = clientId,
            vehicleId = vehicleId,
            companyId = companyId,
            associationType = associationType,
            isPrimary = isPrimary,
            startDate = LocalDateTime.now(),
            endDate = null,
            createdAt = LocalDateTime.now()
        )
    }
}