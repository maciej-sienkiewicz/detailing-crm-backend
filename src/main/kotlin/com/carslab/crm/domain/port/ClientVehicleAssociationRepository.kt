package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId

interface ClientVehicleAssociationRepository {
    fun findVehiclesByClientId(clientId: ClientId): List<VehicleId>

    fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId>

    fun findVehiclesByOwnerIds(ownerIds: List<ClientId>): Map<ClientId, List<VehicleId>>

    fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    fun removeAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    fun hasAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean

    fun countOwnersForVehicle(vehicleId: VehicleId): Int
}