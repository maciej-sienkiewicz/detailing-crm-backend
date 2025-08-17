package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.dto.RelatedEntityDto
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitComment
import org.springframework.stereotype.Service

@Service
class VisitActivitySender(
    private val activityCommandService: ActivityCommandService,
    private val securityContext: SecurityContext
) {
    
    fun onVisitCreated(visit: Visit) {
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.PROTOCOL,
            message = "Utworzono nową wizytę: \"${visit.title}\"",
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw IllegalStateException("User not found in security context"),
            description = "Suma netto: ${visit.totalAmount()} PLN",
            primaryEntity = null,
            relatedEntities = listOf(
                RelatedEntityDto(
                    id = visit.clientId.value.toString(),
                    type = "CLIENT",
                    name = "Imie nazwisko"
                )
            ),
            metadata = mapOf())
        )
    }
    
    fun onCommentAdded(comment: VisitComment) {
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.PROTOCOL,
            message = "Dodano komentarz do wizyty.",
            userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw IllegalStateException("User not found in security context"),
            description = "Treść komentarza: ${comment.content}",
            primaryEntity = null,
            relatedEntities = emptyList(),
            metadata = mapOf())
        )
    }
}