package com.carslab.crm.production.modules.visits.application.service.command.handler.support

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.modules.visits.api.commands.UpdateCarReceptionCommand
import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommentCommandService
import com.carslab.crm.production.modules.visits.domain.command.DeliveryPerson
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.orchestration.VisitCreationOrchestrator
import com.carslab.crm.production.modules.visits.domain.orchestration.VisitEntities
import com.carslab.crm.production.modules.visits.domain.orchestration.ClientDetails
import com.carslab.crm.production.modules.visits.domain.orchestration.VehicleDetails
import org.springframework.stereotype.Component

@Component
class DeliveryPersonHandler(
    private val visitCreationOrchestrator: VisitCreationOrchestrator,
    private val visitCommentCommandService: VisitCommentCommandService
) {

    fun handleDeliveryPersonCreation(request: UpdateCarReceptionCommand, existingVisit: Visit): String? {
        if (!shouldCreateDeliveryPerson(request, existingVisit)) {
            return null
        }

        val deliveryPerson = request.deliveryPerson!!
        val association = createDeliveryPersonAssociation(request, deliveryPerson)

        addDeliveryComment(existingVisit, deliveryPerson)

        return association?.client?.id
    }

    private fun shouldCreateDeliveryPerson(request: UpdateCarReceptionCommand, existingVisit: Visit): Boolean {
        return existingVisit.status == VisitStatus.SCHEDULED &&
                request.status == ApiProtocolStatus.IN_PROGRESS &&
                request.deliveryPerson != null
    }

    private fun createDeliveryPersonAssociation(
        request: UpdateCarReceptionCommand,
        deliveryPerson: DeliveryPerson
    ): VisitEntities? {
        return try {
            val clientDetails = ClientDetails(
                ownerId = deliveryPerson.id?.toLong(),
                name = deliveryPerson.name,
                phone = deliveryPerson.phone
            )

            val vehicleDetails = VehicleDetails(
                make = request.make,
                model = request.model,
                licensePlate = request.licensePlate
            )

            visitCreationOrchestrator.prepareVisitEntities(clientDetails, vehicleDetails)
        } catch (e: Exception) {
            null
        }
    }

    private fun addDeliveryComment(visit: Visit, deliveryPerson: DeliveryPerson) {
        visitCommentCommandService.addComment(
            AddCommentRequest(
                visitId = visit.id!!.value.toString(),
                type = "SYSTEM",
                content = "Pojazd dostarczył/a: ${deliveryPerson.name}, tel: ${deliveryPerson.phone}"
            )
        )
    }
}