package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.production.modules.associations.application.dto.CreateAssociationRequest
import com.carslab.crm.production.modules.associations.application.service.AssociationCommandService
import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.infrastructure.security.SecurityContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VisitCreationOrchestrator(
    private val visitClientResolver: VisitClientResolver,
    private val visitVehicleResolver: VisitVehicleResolver,
    private val associationCommandService: AssociationCommandService,
) {
    private val logger = LoggerFactory.getLogger(VisitCreationOrchestrator::class.java)

    fun prepareVisitEntities(request: CreateVisitRequest): VisitEntities {
        logger.info("Preparing entities for visit: ${request.make} ${request.model}")

        val client = visitClientResolver.resolveClient(request)
        val vehicle = visitVehicleResolver.resolveVehicle(request, client.id.toLong())

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