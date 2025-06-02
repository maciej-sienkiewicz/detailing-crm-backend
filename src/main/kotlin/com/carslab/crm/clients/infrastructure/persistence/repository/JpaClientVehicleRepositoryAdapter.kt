package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.port.ClientVehicleRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaClientVehicleRepositoryAdapter(
    private val clientJpaRepository: ClientJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository
) : ClientVehicleRepository {

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }

    override fun findVehiclesByClientId(clientId: ClientId): List<VehicleId> {
        val companyId = getCurrentCompanyId()
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return emptyList()
        return client.vehicles.filter { it.companyId == companyId }.map { VehicleId(it.id!!) }
    }

    override fun findOwnersByVehicleId(vehicleId: VehicleId): List<ClientId> {
        val companyId = getCurrentCompanyId()
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

    @Transactional
    override fun newAssociation(vehicleId: VehicleId, clientId: ClientId): Boolean {
        val companyId = getCurrentCompanyId()
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return false
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false

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
        val companyId = getCurrentCompanyId()
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return false

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
        val companyId = getCurrentCompanyId()
        val client = clientJpaRepository.findByCompanyIdAndId(companyId, clientId.value).orElse(null) ?: return false
        return client.vehicles.any { it.id == vehicleId.value && it.companyId == companyId }
    }

    override fun countOwnersForVehicle(vehicleId: VehicleId): Int {
        val companyId = getCurrentCompanyId()
        val vehicle = vehicleJpaRepository.findByCompanyIdAndId(companyId, vehicleId.value).orElse(null) ?: return 0
        return vehicle.owners.count { it.companyId == companyId }
    }
}