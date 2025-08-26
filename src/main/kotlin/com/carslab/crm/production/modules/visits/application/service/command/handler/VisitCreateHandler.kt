package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.production.modules.clients.application.dto.UpdateClientRequest
import com.carslab.crm.production.modules.clients.application.service.ClientCommandService
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.mapper.VisitCommandMapper
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import com.carslab.crm.production.modules.visits.domain.orchestration.VisitCreationOrchestrator
import com.carslab.crm.production.modules.visits.domain.orchestration.VisitEntities
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class VisitCreateHandler(
    private val aggregateService: AggregateService,
    private val visitCreationOrchestrator: VisitCreationOrchestrator,
    private val clientCommandService: ClientCommandService,
    private val commandMapper: VisitCommandMapper
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(request: CreateVisitRequest, companyId: Long): VisitResponse {
        val entities = prepareEntities(request)
        updateClientInformation(entities.client.id, request)

        val command = commandMapper.mapCreateCommand(request, companyId, entities)
        val visit = aggregateService.createVisit(command)

        return VisitResponse.from(visit)
    }

    private fun prepareEntities(request: CreateVisitRequest): VisitEntities {
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
}