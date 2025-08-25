package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientRepository
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleOwner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VehicleOwnerResolver(
    private val associationRepository: ClientVehicleAssociationRepository,
    private val clientRepository: ClientRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleOwnerResolver::class.java)

    fun resolveOwners(vehicleIds: List<VehicleId>, companyId: Long): Map<VehicleId, List<VehicleOwner>> {
        if (vehicleIds.isEmpty()) return emptyMap()

        logger.debug("Resolving owners for {} vehicles", vehicleIds.size)

        val associations = associationRepository.findActiveByVehicleIds(vehicleIds)

        val uniqueClientIds = associations.map { it.clientId }.distinct()

        if (uniqueClientIds.isEmpty()) return emptyMap()

        val clients = clientRepository.findByIds(uniqueClientIds, companyId)
        val clientsMap = clients.associateBy { it.id }

        val result = associations
            .groupBy { it.vehicleId }
            .mapValues { (_, vehicleAssociations) ->
                vehicleAssociations
                    .mapNotNull { association -> clientsMap[association.clientId] }
                    .map { client ->
                        VehicleOwner(
                            id = client.id.value,
                            firstName = client.firstName,
                            lastName = client.lastName,
                            fullName = client.fullName,
                            email = client.email,
                            phone = client.phone
                        )
                    }
            }

        logger.debug("Resolved owners using 2 queries total (associations + clients)")
        return result
    }

    fun resolveOwnersForSingleVehicle(vehicleId: VehicleId, companyId: Long): List<VehicleOwner> {
        return resolveOwners(listOf(vehicleId), companyId)[vehicleId] ?: emptyList()
    }
}