package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.handler.support.DeliveryPersonHandler
import com.carslab.crm.production.modules.visits.application.service.command.mapper.VisitCommandMapper
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.visits.domain.service.activity.VisitActivitySender
import org.springframework.stereotype.Component

@Component
class VisitUpdateHandler(
    private val visitDomainService: VisitDomainService,
    private val clientQueryService: ClientQueryService,
    private val vehicleQueryService: VehicleQueryService,
    private val deliveryPersonHandler: DeliveryPersonHandler,
    private val commandMapper: VisitCommandMapper,
    private val activitySender: VisitActivitySender
) {

    fun handle(visitId: VisitId, request: UpdateCarReceptionCommand, companyId: Long): VisitResponse {
        val existingVisit = visitDomainService.getVisitForCompany(visitId, companyId)

        deliveryPersonHandler.handleDeliveryPersonCreation(request, existingVisit)

        val command = commandMapper.mapUpdateCommand(request)
        val updatedVisit = visitDomainService.updateVisit(visitId, command, companyId)

        sendUpdateActivity(updatedVisit, existingVisit, companyId)

        return VisitResponse.from(updatedVisit)
    }

    private fun sendUpdateActivity(
        updatedVisit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit,
        existingVisit: com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit,
        companyId: Long
    ) {
        val client = clientQueryService.getClient(updatedVisit.clientId.value.toString())
        val vehicle = vehicleQueryService.getVehicle(updatedVisit.vehicleId.value.toString())

        activitySender.onVisitUpdated(updatedVisit, existingVisit, client.client, vehicle.vehicle)
    }
}