package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryClientVehicleAssociationRepository {

    private val associations = mutableListOf<AssociationEntity>()

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

    fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        // Check if the association already exists
        val exists = associations.any { it.clientId == clientId && it.vehicleId == vehicleId }
        if (exists) {
            return false
        }
        return associations.add(AssociationEntity(clientId, vehicleId))
    }

    fun removeAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val association = associations.find { it.clientId == clientId && it.vehicleId == vehicleId }
        return if (association != null) {
            associations.remove(association)
            true
        } else {
            false
        }
    }

    fun hasAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        return associations.any { it.clientId == clientId && it.vehicleId == vehicleId }
    }

    fun countOwnersForVehicle(vehicleId: VehicleId): Int {
        return associations.count { it.vehicleId == vehicleId }
    }

    class AssociationEntity(
        val clientId: ClientId,
        val vehicleId: VehicleId
    )
}