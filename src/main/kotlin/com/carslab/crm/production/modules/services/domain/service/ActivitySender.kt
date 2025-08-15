package com.carslab.crm.production.modules.services.domain.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.services.domain.model.Service
import com.carslab.crm.production.shared.exception.UserNotFoundException
import org.springframework.stereotype.Component

@Component
class ActivitySender(
    private val activityCommandService: ActivityCommandService,
    private val securityContext: SecurityContext
) {
    
    fun onServiceCreated(created: Service) =
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.SYSTEM,
            message = "Utworzono nową usługę: \"${created.name}\"",
            userId = securityContext.getCurrentUserId() ?: throw UserNotFoundException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw UserNotFoundException("User not found in security context"),
            description = "NETTO: ${created.price} PLN, BRUTTO: ${created.price * (1.toBigDecimal() + created.vatRate.toBigDecimal() / 100.toBigDecimal())} PLN, Stawka VAT: ${created.vatRate}%",
            primaryEntity = null,
            relatedEntities = emptyList(),
            metadata = mapOf()
        ))
    
    fun onServiceChanged(previous: Service, updated: Service) =
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.SYSTEM,
            message = "Zaktualizowano usługę: \"${updated.name}\" (poprzednia nazwa: \"${previous.name}\")",
            userId = securityContext.getCurrentUserId() ?: throw UserNotFoundException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw UserNotFoundException("User not found in security context"),
            description = detectChanges(previous, updated),
            primaryEntity = null,
            relatedEntities = emptyList(),
            metadata = mapOf()
        ))
    
    fun onServiceDeleted(deleted: Service) =
        activityCommandService.createActivity(CreateActivityRequest(
            category = ActivityCategory.SYSTEM,
            message = "Usunięto usługę: \"${deleted.name}\"",
            userId = securityContext.getCurrentUserId() ?: throw UserNotFoundException("User not found in security context"),
            userName = securityContext.getCurrentUserName() ?: throw UserNotFoundException("User not found in security context"),
            primaryEntity = null,
            relatedEntities = emptyList(),
            metadata = mapOf()
        ))

    private fun detectChanges(
        previous: Service,
        updated: Service
    ): String? =
        buildString {
            if (previous.name != updated.name) append("Nazwa: ${previous.name} -> ${updated.name}\n")
            if (previous.description != updated.description) append("Opis: ${previous.description} -> ${updated.description}\n")
            if (previous.price != updated.price) append("Cena netto: ${previous.price} PLN -> ${updated.price} PLN\n")
            if (previous.vatRate != updated.vatRate) append("Stawka VAT: ${previous.vatRate}% -> ${updated.vatRate}%\n")
            if (previous.isActive != updated.isActive) append("Aktywność: ${if (previous.isActive) "Aktywna" else "Nieaktywna"} -> ${if (updated.isActive) "Aktywna" else "Nieaktywna"}\n")
        }.takeIf { it.isNotEmpty() }
}