package com.carslab.crm.modules.clients.domain.port

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientVehicleAssociation
import com.carslab.crm.modules.clients.domain.model.VehicleId

interface ClientVehicleAssociationRepository {
    fun save(association: ClientVehicleAssociation): ClientVehicleAssociation
    fun findByClientId(clientId: ClientId): List<ClientVehicleAssociation>
    fun findByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation>
    fun findActiveByClientId(clientId: ClientId): List<ClientVehicleAssociation>
    fun findActiveByVehicleId(vehicleId: VehicleId): List<ClientVehicleAssociation>
    fun findByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): ClientVehicleAssociation?
    fun deleteByClientIdAndVehicleId(clientId: ClientId, vehicleId: VehicleId): Boolean
    fun deleteByClientId(clientId: ClientId): Int
    fun deleteByVehicleId(vehicleId: VehicleId): Int
}