package com.carslab.crm.production.modules.visits.domain.service.aggregate

import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.vehicles.application.service.VehicleStatisticsCommandService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.command.CreateVisitCommand
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.activity.VisitActivitySender
import com.carslab.crm.production.modules.visits.domain.factory.VisitFactory
import com.carslab.crm.production.modules.visits.domain.validator.VisitCommandValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VisitCreationService(
    private val visitRepository: VisitRepository,
    private val visitFactory: VisitFactory,
    private val commandValidator: VisitCommandValidator,
    private val clientStatisticsService: ClientStatisticsCommandService,
    private val vehicleStatisticsCommandService: VehicleStatisticsCommandService,
    private val activitySender: VisitActivitySender
) {
    private val logger = LoggerFactory.getLogger(VisitCreationService::class.java)

    fun createVisit(command: CreateVisitCommand): Visit {
        commandValidator.validateCreateCommand(command)

        val visit = visitFactory.createVisit(command)
        val savedVisit = visitRepository.save(visit)

        executePostCreationActions(savedVisit, command)

        return savedVisit
    }

    private fun executePostCreationActions(visit: Visit, command: CreateVisitCommand) {
        recordClientStatistics(command.client.id)
        recordVehicleStatistics(command.vehicle.id)
        publishVisitCreatedEvent(visit, command.client, command.vehicle)
    }

    private fun recordClientStatistics(clientId: String) {
        try {
            clientStatisticsService.recordVisit(clientId)
        } catch (e: Exception) {
            logger.error("Failed to record client statistics for client: $clientId", e)
        }
    }

    private fun recordVehicleStatistics(vehicleId: String) {
        try {
            vehicleStatisticsCommandService.recordVisit(VehicleId(vehicleId.toLong()))
        } catch (e: Exception) {
            logger.error("Failed to record client statistics for client: $vehicleId", e)
        }
    }

    private fun publishVisitCreatedEvent(
        visit: Visit,
        client: ClientResponse,
        vehicle: VehicleResponse
    ) {
        try {
            activitySender.onVisitCreated(visit, client, vehicle)
        } catch (e: Exception) {
            logger.error("Failed to publish visit created event for visit: ${visit.id}", e)
        }
    }
}