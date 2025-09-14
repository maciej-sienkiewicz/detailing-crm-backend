package com.carslab.crm.production.modules.events.domain.services

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.request.ServiceApprovalStatus
import com.carslab.crm.production.modules.events.application.dto.ConvertToVisitRequest
import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class EventToVisitConversionService(
    private val recurringEventRepository: RecurringEventRepository,
    private val securityContext: SecurityContext
) {

    fun buildVisitRequestFromOccurrence(
        occurrence: EventOccurrence,
        conversionRequest: ConvertToVisitRequest
    ): CreateVisitRequest {
        val companyId = securityContext.getCurrentCompanyId()

        val recurringEvent = recurringEventRepository.findById(occurrence.recurringEventId, companyId)
            ?: throw EntityNotFoundException("Recurring event not found: ${occurrence.recurringEventId}")

        if (!recurringEvent.isRecurringVisit()) {
            throw BusinessException("Cannot convert simple event to visit")
        }

        val template = recurringEvent.visitTemplate
            ?: throw BusinessException("Visit template is required for recurring visits")

        val scheduledDate = occurrence.scheduledDate
        val endDate = scheduledDate.plus(template.estimatedDuration)

        validateConversionRequest(conversionRequest, template)

        val defaultServices = template.defaultServices.map { serviceTemplate ->
            com.carslab.crm.modules.visits.api.commands.CreateServiceCommand(
                id = "",
                name = serviceTemplate.name,
                price = serviceTemplate.basePrice,
                quantity = 1,
                discountType = null,
                discountValue = null,
                finalPrice = null,
                approvalStatus = ServiceApprovalStatus.APPROVED,
                note = null
            )
        }

        val additionalServices = conversionRequest.additionalServices.map { additionalService ->
            com.carslab.crm.modules.visits.api.commands.CreateServiceCommand(
                id = "",
                name = additionalService.name,
                price = additionalService.basePrice,
                quantity = additionalService.quantity,
                discountType = null,
                discountValue = null,
                finalPrice = null,
                approvalStatus = ServiceApprovalStatus.APPROVED,
                note = null
            )
        }

        return CreateVisitRequest(
            title = recurringEvent.title,
            calendarColorId = "default",
            startDate = scheduledDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            endDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            licensePlate = getVehicleLicensePlate(conversionRequest.vehicleId),
            make = getVehicleMake(conversionRequest.vehicleId),
            model = getVehicleModel(conversionRequest.vehicleId),
            productionYear = null,
            mileage = null,
            vin = null,
            color = null,
            keysProvided = false,
            documentsProvided = false,
            ownerId = conversionRequest.clientId,
            ownerName = getClientName(conversionRequest.clientId),
            companyName = null,
            taxId = null,
            address = null,
            email = null,
            phone = null,
            notes = occurrence.notes,
            selectedServices = defaultServices + additionalServices,
            status = com.carslab.crm.api.model.ApiProtocolStatus.SCHEDULED,
            referralSource = null,
            otherSourceDetails = null,
            appointmentId = null,
            deliveryPerson = null
        )
    }

    private fun validateConversionRequest(
        request: ConvertToVisitRequest,
        template: com.carslab.crm.production.modules.events.domain.models.value_objects.VisitTemplate
    ) {
        if (template.clientId != null && template.clientId != request.clientId) {
            throw BusinessException("Client ID does not match template")
        }
        if (template.vehicleId != null && template.vehicleId != request.vehicleId) {
            throw BusinessException("Vehicle ID does not match template")
        }
    }

    private fun getVehicleLicensePlate(vehicleId: Long): String? {
        return null
    }

    private fun getVehicleMake(vehicleId: Long): String {
        return "Unknown"
    }

    private fun getVehicleModel(vehicleId: Long): String {
        return "Unknown"
    }

    private fun getClientName(clientId: Long): String {
        return "Client"
    }

    private fun buildVisitNotes(
        recurringEvent: com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent,
        occurrence: EventOccurrence,
        conversionRequest: ConvertToVisitRequest
    ): String {
        val notes = mutableListOf<String>()

        notes.add("Converted from recurring event: ${recurringEvent.title}")
        notes.add("Original scheduled date: ${occurrence.scheduledDate}")

        recurringEvent.description?.let { notes.add("Event description: $it") }
        recurringEvent.visitTemplate?.notes?.let { notes.add("Template notes: $it") }
        occurrence.notes?.let { notes.add("Occurrence notes: $it") }
        conversionRequest.notes?.let { notes.add("Conversion notes: $it") }

        return notes.joinToString("\n")
    }
}