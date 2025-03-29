package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.ClientStats
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryClientVehicleAssociationRepository {

    private val associations = mutableListOf<AssociationEntity> ()


    fun findVehiclesByClientId(clientId: ClientId): List<VehicleId> {
        return associations.filter { it.clientId == clientId }.map { it.vehicleId }
    }

    fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId> {
        return associations.filter { it.vehicleId == vehicleId}.map { it.clientId }
    }

    fun findVehiclesByOwnerIds(ownerIds: List<ClientId>): Map<ClientId, List<VehicleId>> {
        return associations.filter { it.clientId in ownerIds }
            .groupBy { it.clientId }
            .mapValues { it.value.map { association -> association.vehicleId } }
    }

    fun newAssociation(vehicleId: VehicleId, clientId: ClientId) =
        associations.add(AssociationEntity(clientId, vehicleId))

    class AssociationEntity(
        val clientId: ClientId,
        val vehicleId: VehicleId
    )
}