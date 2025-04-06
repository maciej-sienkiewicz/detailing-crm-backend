package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.port.ClientVehicleRepository
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaClientVehicleRepositoryAdapter(
    private val clientJpaRepository: ClientJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository
) : ClientVehicleRepository {

    override fun findVehiclesByClientId(clientId: ClientId): List<VehicleId> {
        val client = clientJpaRepository.findById(clientId.value).orElse(null) ?: return emptyList()
        return client.vehicles.map { VehicleId(it.id!!) }
    }

    override fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId> {
        val vehicle = vehicleJpaRepository.findById(vehicleId.value).orElse(null) ?: return emptyList()
        return vehicle.owners.map { ClientId(it.id!!) }
    }

    override fun findVehiclesByOwnerIds(ownerIds: List<ClientId>): Map<ClientId, List<VehicleId>> {
        val result = mutableMapOf<ClientId, List<VehicleId>>()

        for (clientId in ownerIds) {
            val vehicles = findVehiclesByClientId(clientId)
            if (vehicles.isNotEmpty()) {
                result[clientId] = vehicles
            }
        }

        return result
    }

    @Transactional
    override fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val vehicle = vehicleJpaRepository.findById(vehicleId.value).orElse(null) ?: return false
        val client = clientJpaRepository.findById(clientId.value).orElse(null) ?: return false

        // Check if the association already exists
        if (client.vehicles.any { it.id == vehicleId.value }) {
            return false
        }

        val hasClientSideAssociation = client.vehicles.contains(vehicle)
        val hasVehicleSideAssociation = vehicle.owners.contains(client)
        if(!hasClientSideAssociation) client.vehicles.add(vehicle)
        if(!hasVehicleSideAssociation) vehicle.owners.add(client)

        if(!hasClientSideAssociation || !hasVehicleSideAssociation) {
            clientJpaRepository.save(client)
            vehicleJpaRepository.save(vehicle)
        }

        return true
    }

    override fun removeAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val client = clientJpaRepository.findById(clientId.value).orElse(null) ?: return false

        // Check if the association exists
        if (!client.vehicles.any { it.id == vehicleId.value }) {
            return false
        }

        // Remove the association
        client.vehicles.removeIf { it.id == vehicleId.value }
        clientJpaRepository.save(client)

        return true
    }

    override fun hasAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val client = clientJpaRepository.findById(clientId.value).orElse(null) ?: return false
        return client.vehicles.any { it.id == vehicleId.value }
    }

    override fun countOwnersForVehicle(vehicleId: VehicleId): Int {
        val vehicle = vehicleJpaRepository.findById(vehicleId.value).orElse(null) ?: return 0
        return vehicle.owners.size
    }
}