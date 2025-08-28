package com.carslab.crm.production.modules.vehicles.application.service.handler

import com.carslab.crm.production.modules.associations.application.service.AssociationCommandService
import com.carslab.crm.production.modules.associations.application.service.AssociationQueryService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.springframework.stereotype.Service

@Service
class UpdateVehicleOwnersHandler(
    private val associationQueryService: AssociationQueryService,
    private val associationCommandService: AssociationCommandService
) {
    
    fun handle(vehicleId: VehicleId, requestedOwners: List<ClientId>) {
        val existingOwners = associationQueryService.getVehicleClients(vehicleId.value.toString())
        if(existingOwners.toSet() != requestedOwners.toSet()) {
            associationCommandService.updateVehicleOwners(vehicleId, requestedOwners)
        }
    }
}