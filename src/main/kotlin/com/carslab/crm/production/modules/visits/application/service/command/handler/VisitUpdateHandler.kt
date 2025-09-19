package com.carslab.crm.production.modules.visits.application.service.command.handler

import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.service.command.handler.support.DeliveryPersonHandler
import com.carslab.crm.production.modules.visits.application.service.command.mapper.VisitCommandMapper
import com.carslab.crm.production.modules.visits.domain.activity.VisitActivitySender
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.AggregateService
import org.springframework.stereotype.Component

@Component
class VisitUpdateHandler(
    private val visitDomain: AggregateService,
    private val deliveryPersonHandler: DeliveryPersonHandler,
    private val clientQueryService: ClientQueryService,
    private val vehicleQueryService: VehicleQueryService,
    private val commandMapper: VisitCommandMapper,
    private val activitySender: VisitActivitySender
) {

    fun handle(visitId: VisitId, request: UpdateCarReceptionCommand, companyId: Long): VisitResponse {
        val existingVisit = visitDomain.findById(visitId, companyId)

        val deliveryPerson = request.deliveryPerson
        val updatedDeliveryPerson: DeliveryPerson? = if(deliveryPerson != null) {
            val newId = deliveryPersonHandler.handleDeliveryPersonCreation(request, existingVisit)
            deliveryPerson.copy(id = newId ?: deliveryPerson.id)
        } else {
            null
        }
        
        val command = commandMapper.mapUpdateCommand(request.copy(deliveryPerson = updatedDeliveryPerson))
        val updatedVisit = visitDomain.updateVisit(visitId, command, companyId)

        sendUpdateActivity(updatedVisit, existingVisit)

        return VisitResponse.from(updatedVisit)
    }

    private fun sendUpdateActivity(
        updatedVisit: Visit,
        existingVisit: Visit,
    ) {
        val client = clientQueryService.getClient(updatedVisit.clientId.value.toString())
        val vehicle = vehicleQueryService.getVehicle(updatedVisit.vehicleId.value.toString())

        activitySender.onVisitUpdated(updatedVisit, existingVisit, client.client, vehicle.vehicle)
    }
}