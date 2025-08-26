package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.mapper.VisitCommandMapper
import com.carslab.crm.production.modules.visits.domain.service.VisitCreationOrchestrator
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.service.activity.VisitActivitySender
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class VisitCreateHandler(
    private val visitDomainService: VisitDomainService,
    private val visitCreationOrchestrator: VisitCreationOrchestrator,
    private val clientCommandService: ClientCommandService,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
    private val commandMapper: VisitCommandMapper,
    private val activitySender: VisitActivitySender
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(request: CreateVisitRequest, companyId: Long): VisitResponse {
        val entities = prepareEntities(request)
        updateClientInformation(entities.client.id, request)

        val command = commandMapper.mapCreateCommand(request, companyId, entities)
        val visit = visitDomainService.createVisit(command)

        recordClientStatistics(entities.client.id)
        sendActivity(visit, entities)

        return VisitResponse.from(visit)
    }

    private fun prepareEntities(request: CreateVisitRequest): com.carslab.crm.production.modules.visits.domain.service.VisitEntities {
        val clientDetails = commandMapper.mapClientDetails(request)
        val vehicleDetails = commandMapper.mapVehicleDetails(request)

        return visitCreationOrchestrator.prepareVisitEntities(clientDetails, vehicleDetails)
    }

    private fun updateClientInformation(clientId: String, request: CreateVisitRequest) {
        val updateRequest = createUpdateClientRequest(request)
        clientCommandService.updateClient(clientId, updateRequest)
    }

    private fun createUpdateClientRequest(request: CreateVisitRequest): UpdateClientRequest {
        val nameParts = request.ownerName.split(" ", limit = 2)

        return UpdateClientRequest(
            firstName = nameParts.getOrElse(0) { "" },
            lastName = nameParts.getOrElse(1) { "" },
            email = request.email ?: "",
            phone = request.phone ?: "",
            address = request.address,
            company = request.companyName,
            taxId = request.taxId
        )
    }

    private fun recordClientStatistics(clientId: String) {
        clientStatisticsCommandService.recordVisit(clientId)
    }

    private fun sendActivity(
        visit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit,
        entities: com.carslab.crm.production.modules.visits.domain.service.VisitEntities
    ) {
        activitySender.onVisitCreated(visit, entities.client, entities.vehicle)
    }
}