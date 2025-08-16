package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.activities.application.dto.CreateActivityRequest
import com.carslab.crm.production.modules.activities.application.service.ActivityCommandService
import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.clients.domain.model.Client
import org.springframework.stereotype.Component

@Component
class ClientActivitySender(
    private val activityCommandService: ActivityCommandService,
    private val securityContext: SecurityContext
) {
    fun onClientCreated(client: Client) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.CLIENT,
                message = "Utworzono nowego klienta: \"${client.fullName}\"",
                description = "Email: ${client.email}, Telefon: ${client.phone}",
                userId = securityContext.getCurrentUserId()
                    ?: throw IllegalStateException("User not found in security context"),
                userName = securityContext.getCurrentUserName()
                    ?: throw IllegalStateException("User not found in security context")
            )
        )
    }
    
    fun onClientUpdated(previous: Client, updated: Client) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.CLIENT,
                message = "Zaktualizowano klienta: \"${updated.fullName}\"",
                description = detectChanges(previous, updated),
                userId = securityContext.getCurrentUserId()
                    ?: throw IllegalStateException("User not found in security context"),
                userName = securityContext.getCurrentUserName()
                    ?: throw IllegalStateException("User not found in security context")
            )
        )
    }
    
    fun onClientDeleted(client: Client) {
        activityCommandService.createActivity(
            CreateActivityRequest(
                category = ActivityCategory.CLIENT,
                message = "Usunięto klienta: \"${client.fullName}\"",
                userId = securityContext.getCurrentUserId()
                    ?: throw IllegalStateException("User not found in security context"),
                userName = securityContext.getCurrentUserName()
                    ?: throw IllegalStateException("User not found in security context")
            )
        )
    }

    private fun detectChanges(
        previous: Client,
        updated: Client
    ): String? {
        return buildString {
            if (previous.firstName != updated.firstName) append("Imię: ${previous.firstName} -> ${updated.firstName}\n")
            if (previous.lastName != updated.lastName) append("Nazwisko: ${previous.lastName} -> ${updated.lastName}\n")
            if (previous.email != updated.email) append("Email: ${previous.email} -> ${updated.email}\n")
            if (previous.phone != updated.phone) append("Telefon: ${previous.phone} -> ${updated.phone}\n")
            if (previous.address != updated.address) append("Adres: ${previous.address} -> ${updated.address}\n")
            if (previous.company != updated.company) append("Firma: ${previous.company} -> ${updated.company}\n")
            if (previous.taxId != updated.taxId) append("NIP: ${previous.taxId} -> ${updated.taxId}\n")
            if (previous.notes != updated.notes) append("Notatki: ${previous.notes} -> ${updated.notes}\n")
        }.ifEmpty { null }
    }
}