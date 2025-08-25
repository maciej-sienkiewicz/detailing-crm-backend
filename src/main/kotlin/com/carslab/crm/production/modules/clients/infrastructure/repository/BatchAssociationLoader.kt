package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.associations.domain.repository.ClientVehicleAssociationRepository
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BatchAssociationLoader(
    private val associationRepository: ClientVehicleAssociationRepository
) {
    private val logger = LoggerFactory.getLogger(BatchAssociationLoader::class.java)

    fun enrichWithVehicleAssociations(
        clientsWithStats: List<ClientWithStatistics>,
        companyId: Long
    ): List<ClientWithStatistics> {
        if (clientsWithStats.isEmpty()) return clientsWithStats

        logger.debug("Enriching {} clients with vehicle associations using batch loading", clientsWithStats.size)

        val clientIds = clientsWithStats.map { it.client.id }
        val associations = associationRepository.findActiveByClientIds(clientIds)

        val clientVehicleMap = associations
            .groupBy { it.clientId }
            .mapValues { (_, clientAssociations) ->
                clientAssociations.map { it.vehicleId.value }
            }

        val enrichedClients = clientsWithStats.map { clientWithStats ->
            val vehicleIds = clientVehicleMap[clientWithStats.client.id] ?: emptyList()
            clientWithStats.copy(vehicleIds = vehicleIds)
        }

        logger.debug("Enriched clients with vehicle associations using single batch query")
        return enrichedClients
    }

    fun loadVehicleCountsForClients(clientIds: List<ClientId>): Map<ClientId, Long> {
        if (clientIds.isEmpty()) return emptyMap()

        logger.debug("Batch loading vehicle counts for {} clients", clientIds.size)

        val associations = associationRepository.findActiveByClientIds(clientIds)
        val vehicleCounts = associations
            .groupBy { it.clientId }
            .mapValues { (_, clientAssociations) -> clientAssociations.size.toLong() }

        logger.debug("Loaded vehicle counts for {} clients in single query", vehicleCounts.size)
        return vehicleCounts
    }
}