package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaClientVehicleAssociationAdapter(
    private val clientJpaRepository: ClientJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
) : ClientVehicleAssociationRepository {

    override fun findVehiclesByClientId(clientId: ClientId): List<VehicleId> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return emptyList()
        return client.vehicles.filter { it.companyId == companyId }.map { VehicleId(it.id!!) }
    }

    override fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return emptyList()
        return vehicle.owners.filter { it.companyId == companyId }.map { ClientId(it.id!!) }
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

    override fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return false
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false

        // Check if the association already exists
        if (client.vehicles.any { it.id == vehicleId.value }) {
            return false
        }

        // Add the association
        client.vehicles.add(vehicle)
        clientJpaRepository.save(client)

        return true
    }

    override fun removeAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return false
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false

        // Check if the association exists
        if (!client.vehicles.any { it.id == vehicleId.value }) {
            return false
        }

        // Remove the association
        client.vehicles.removeIf { it.id == vehicleId.value }
        vehicle.owners.removeIf { it.id == clientId.value }

        clientJpaRepository.save(client)
        vehicleJpaRepository.save(vehicle)

        return true
    }

    override fun hasAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false
        return client.vehicles.any { it.id == vehicleId.value && it.companyId == companyId }
    }

    override fun countOwnersForVehicle(vehicleId: VehicleId): Int {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return 0
        return vehicle.owners.count { it.companyId == companyId }
    }
}