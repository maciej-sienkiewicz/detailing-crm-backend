package com.carslab.crm.production.modules.associations.domain.repository

import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId

interface ClientVehicleAssociationRepository {
    fun save(association: ClientVehicleAssociation): ClientVehicleAssociation
    fun findActiveByClientId(clientId: ClientId): List<ClientVehicleAssociation>
    fun findActiveByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation>
    fun findByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): ClientVehicleAssociation?
    fun endAssociation(clientId: ClientId, vehicleId: VehicleId): Boolean
    fun deleteByClientId(clientId: ClientId): Int
    fun deleteByVehicleId(vehicleId: VehicleId): Int

    fun findActiveByClientIds(clientIds: List<ClientId>): List<ClientVehicleAssociation>
    fun findActiveByVehicleIds(vehicleIds: List<VehicleId>): List<ClientVehicleAssociation>
    fun batchEndAssociations(associations: List<Pair<ClientId, VehicleId>>): Int
}