package com.carslab.crm.production.modules.visits.domain.orchestration

import com.carslab.crm.production.modules.associations.application.dto.CreateAssociationRequest
import com.carslab.crm.production.modules.associations.application.service.AssociationCommandService
import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VisitCreationOrchestrator(
    private val clientResolver: ClientResolver,
    private val vehicleResolver: VehicleResolver,
    private val associationCommandService: AssociationCommandService,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
) {
    private val logger = LoggerFactory.getLogger(VisitCreationOrchestrator::class.java)

    fun prepareVisitEntities(clientDetails: ClientDetails, vehicleDetails: VehicleDetails): VisitEntities {
        logger.info("Preparing entities for visit: ${vehicleDetails.make} ${vehicleDetails.model}")

        val client = clientResolver.resolveClient(clientDetails)
        val vehicle = vehicleResolver.resolveVehicle(vehicleDetails.copy(ownerId = client.id.toLong() ))

        ensureClientVehicleAssociation(client, vehicle)

        logger.info("Entities prepared: client=${client.id}, vehicle=${vehicle.id}")
        return VisitEntities(client, vehicle)
    }

    private fun ensureClientVehicleAssociation(client: ClientResponse, vehicle: VehicleResponse) {
        try {
            associationCommandService.createAssociation(
                CreateAssociationRequest(
                    clientId = client.id.toLong(),
                    vehicleId = vehicle.id.toLong(),
                    associationType = AssociationType.OWNER,
                    isPrimary = true
                )
            )
                .also { clientStatisticsCommandService.incrementVehicleCount(client.id) }
            logger.debug("Created association between client: {} and vehicle: {}", client.id, vehicle.id)
        } catch (e: Exception) {
            logger.debug("Association between client: {} and vehicle: {} might already exist", client.id, vehicle.id)
        }
    }
}

data class VisitEntities(
    val client: ClientResponse,
    val vehicle: VehicleResponse
)