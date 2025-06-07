package com.carslab.crm.clients.domain

import com.carslab.crm.domain.exception.DomainException
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.clients.domain.model.ClientSummary
import com.carslab.crm.clients.domain.model.ClientVehicleAssociation
import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.model.VehicleRelationshipType
import com.carslab.crm.clients.domain.port.ClientRepository
import com.carslab.crm.clients.domain.port.ClientStatisticsRepository
import com.carslab.crm.clients.domain.port.ClientVehicleAssociationRepository
import com.carslab.crm.clients.domain.port.VehicleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ClientVehicleAssociationService(
    private val associationRepository: ClientVehicleAssociationRepository,
    private val clientRepository: ClientRepository,
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository
) {

    fun associateClientWithVehicle(
        clientId: ClientId,
        vehicleId: VehicleId,
        relationshipType: VehicleRelationshipType = VehicleRelationshipType.OWNER,
        isPrimary: Boolean = false
    ): ClientVehicleAssociation {

        // Validate entities exist
        clientRepository.findById(clientId)
            ?: throw DomainException("Client not found: ${clientId.value}")
        vehicleRepository.findById(vehicleId)
            ?: throw DomainException("Vehicle not found: ${vehicleId.value}")

        // Check if association already exists
        val existing = associationRepository.findByClientIdAndVehicleId(clientId, vehicleId)
        if (existing != null && existing.endDate == null) {
            throw DomainException("Active association already exists between client ${clientId.value} and vehicle ${vehicleId.value}")
        }

        val association = ClientVehicleAssociation(
            clientId = clientId,
            vehicleId = vehicleId,
            relationshipType = relationshipType,
            isPrimary = isPrimary
        )

        val savedAssociation = associationRepository.save(association)

        // Update client vehicle count
        clientStatisticsRepository.updateVehicleCount(clientId, 1)

        return savedAssociation
    }

    fun removeAssociation(clientId: ClientId, vehicleId: VehicleId): Boolean {
        val removed = associationRepository.deleteByClientIdAndVehicleId(clientId, vehicleId)

        if (removed) {
            // Update client vehicle count
            clientStatisticsRepository.updateVehicleCount(clientId, -1)
        }

        return removed
    }

    @Transactional(readOnly = true)
    fun getClientVehicles(clientId: ClientId): List<Vehicle> {
        val associations = associationRepository.findActiveByClientId(clientId)
        val vehicleIds = associations.map { it.vehicleId }
        return vehicleRepository.findByIds(vehicleIds)
    }

    @Transactional(readOnly = true)
    fun getVehicleOwners(vehicleId: VehicleId): List<ClientSummary> {
        val associations = associationRepository.findActiveByVehicleId(vehicleId)
        val clientIds = associations.map { it.clientId }
        return clientRepository.findByIds(clientIds).map { client ->
            ClientSummary(
                id = client.id,
                fullName = client.fullName,
                email = client.email,
                phone = client.phone
            )
        }
    }
}