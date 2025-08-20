package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.dto.RelatedEntityDto
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.clients.application.dto.ClientResponse
import com.carslab.crm.production.modules.vehicles.application.dto.VehicleResponse
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.stereotype.Service

@Service
class VisitActivitySender(
    private val activityCommandService: ActivityCommandService,
    private val securityContext: SecurityContext
) {

    fun onVisitCreated(visit: Visit, client: ClientResponse, vehicle: VehicleResponse) {
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.PROTOCOL,
            message = "Utworzono nową wizytę: \"${visit.title}\"",
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw IllegalStateException("User not found in security context"),
            description = "Suma netto: ${visit.totalAmount()} PLN",
            primaryEntity = RelatedEntityDto(
                id = visit.id.toString(),
                type = "VISIT",
                name = visit.title
            ),
            relatedEntities = listOf(
                RelatedEntityDto(
                    id = visit.clientId.value.toString(),
                    type = "CLIENT",
                    name = client.fullName
                ),
                RelatedEntityDto(
                    id = visit.vehicleId.value.toString(),
                    type = "VEHICLE",
                    name = "${vehicle.make} ${vehicle.model} (${vehicle.year})"
                )
            ),
            metadata = mapOf())
        )
    }

    fun onCommentAdded(comment: VisitComment, visitId: VisitId, title: String) {
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.PROTOCOL,
            message = "Dodano komentarz do wizyty.",
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw IllegalStateException("User not found in security context"),
            description = "Treść komentarza: ${comment.content}",
            primaryEntity = RelatedEntityDto(
                id = visitId.value.toString(),
                type = "VISIT",
                name = title
            ),
            relatedEntities = emptyList(),
            metadata = mapOf())
        )
    }

    fun onVisitUpdated(newVisit: Visit, existingVisit: Visit, client: ClientResponse, vehicle: VehicleResponse) {
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.PROTOCOL,
            message = "Zaktualizowano wizytę: \"${newVisit.title}\"",
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw IllegalStateException("User not found in security context"),
            description = "Zmiany: ${detectChanges(existingVisit, newVisit)}",
            primaryEntity = RelatedEntityDto(
                id = newVisit.id.toString(),
                type = "VISIT",
                name = newVisit.title
            ),
            relatedEntities = listOf(
                RelatedEntityDto(
                    id = newVisit.clientId.value.toString(),
                    type = "CLIENT",
                    name = client.fullName
                ),
                RelatedEntityDto(
                    id = newVisit.vehicleId.value.toString(),
                    type = "VEHICLE",
                    name = "${vehicle.make} ${vehicle.model} (${vehicle.year})"
                )
            ),
            metadata = mapOf())
        )
    }

    private fun detectChanges(
        previous: Visit,
        updated: Visit
    ): String? {
        return buildString {
            if (previous.title != updated.title) {
                append("Tytuł: '${previous.title}' -> '${updated.title}'\n")
            }
            if (previous.notes != updated.notes) {
                append("Notatki: '${previous.notes}' -> '${updated.notes}'\n")
            }
            if (previous.status != updated.status) {
                append("Status: '${previous.status}' -> '${updated.status}'\n")
            }
            if (servicesChanged(previous, updated)) {
                append("Liczba usług: ${previous.services.size} -> ${updated.services.size}\n")
                append("Końcowa cena: ${previous.totalAmount()} PLN -> ${updated.totalAmount()} PLN\n")
            }
        }.takeIf { it.isNotEmpty() }
    }

    private fun servicesChanged(
        previous: Visit,
        updated: Visit
    ): Boolean {
        if (previous.services.size != updated.services.size) return true
        return previous.services.zip(updated.services).any { (oldService, newService) ->
            oldService.name != newService.name ||
            oldService.basePrice != newService.basePrice ||
            oldService.finalPrice != newService.finalPrice ||
            oldService.approvalStatus != newService.approvalStatus ||
            oldService.note != newService.note
        }
    }
}
