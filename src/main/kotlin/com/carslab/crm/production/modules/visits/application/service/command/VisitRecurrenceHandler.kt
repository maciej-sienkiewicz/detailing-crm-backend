package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.production.modules.events.application.dto.CreateRecurringEventRequest
import com.carslab.crm.production.modules.events.application.dto.RecurrencePatternRequest as EventRecurrencePatternRequest
import com.carslab.crm.production.modules.events.application.dto.ServiceTemplateRequest
import com.carslab.crm.production.modules.events.application.dto.VisitTemplateRequest
import com.carslab.crm.production.modules.events.application.service.command.RecurringEventCommandService
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitResponse
import com.carslab.crm.production.modules.visits.application.dto.fromRecurringEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class VisitRecurrenceHandler(
    private val recurringEventCommandService: RecurringEventCommandService
) {
    private val logger = LoggerFactory.getLogger(VisitRecurrenceHandler::class.java)

    fun handleRecurringVisit(request: CreateVisitRequest): VisitResponse {
        logger.info("Creating recurring visit: {}", request.title)

        val recurringEventRequest = mapToRecurringEventRequest(request)
        val recurringEvent = recurringEventCommandService.createRecurringEvent(recurringEventRequest)

        return VisitResponse.fromRecurringEvent(recurringEvent, request)
    }

    private fun mapToRecurringEventRequest(request: CreateVisitRequest): CreateRecurringEventRequest {
        val recurrencePattern = EventRecurrencePatternRequest(
            frequency = request.recurrencePattern!!.frequency,
            interval = request.recurrencePattern.interval,
            daysOfWeek = request.recurrencePattern.daysOfWeek,
            dayOfMonth = request.recurrencePattern.dayOfMonth,
            endDate = request.recurrencePattern.endDate,
            maxOccurrences = request.recurrencePattern.maxOccurrences
        )

        val startDate = LocalDateTime.parse(request.startDate)
        val endDate = request.endDate?.let { LocalDateTime.parse(it) } ?: startDate.plusHours(1)
        val estimatedDuration = Duration.between(startDate, endDate)

        val visitTemplate = VisitTemplateRequest(
            clientId = request.ownerId,
            vehicleId = null,
            estimatedDurationMinutes = estimatedDuration.toMinutes(),
            defaultServices = request.selectedServices?.map { service ->
                ServiceTemplateRequest(
                    name = service.name,
                    basePrice = service.price.priceBrutto
                )
            } ?: emptyList(),
            notes = request.notes
        )

        return CreateRecurringEventRequest(
            title = request.title,
            description = "Recurring visit created from visit request",
            type = "RECURRING_VISIT",
            recurrencePattern = recurrencePattern,
            visitTemplate = visitTemplate
        )
    }
}