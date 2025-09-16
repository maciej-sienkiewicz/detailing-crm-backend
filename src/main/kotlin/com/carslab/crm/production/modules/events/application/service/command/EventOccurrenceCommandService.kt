package com.carslab.crm.production.modules.events.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.*
import com.carslab.crm.production.modules.events.domain.activity.EventActivitySender
import com.carslab.crm.production.modules.events.domain.factory.EventOccurrenceFactory
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.modules.events.domain.services.EventToVisitConversionService
import com.carslab.crm.production.modules.visits.application.dto.CreateVisitRequest
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventOccurrenceCommandService(
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val eventOccurrenceFactory: EventOccurrenceFactory,
    private val eventToVisitConversionService: EventToVisitConversionService,
    private val recurringEventRepository: RecurringEventRepository,
    private val securityContext: SecurityContext,
    private val eventActivitySender: EventActivitySender
) {
    private val logger = LoggerFactory.getLogger(EventOccurrenceCommandService::class.java)

    fun updateOccurrenceStatus(
        occurrenceId: Long,
        request: UpdateOccurrenceStatusRequest
    ): EventOccurrenceResponse {
        logger.info("Updating occurrence status: {} to {}", occurrenceId, request.status)

        val occurrence = eventOccurrenceRepository.findById(EventOccurrenceId.of(occurrenceId))
            ?: throw EntityNotFoundException("Event occurrence not found: $occurrenceId")

        val previousStatus = occurrence.status

        val newStatus = try {
            OccurrenceStatus.valueOf(request.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid occurrence status: ${request.status}")
        }

        val updatedOccurrence = eventOccurrenceFactory.updateOccurrence(
            existingOccurrence = occurrence,
            status = newStatus,
            notes = request.notes
        )

        val savedOccurrence = eventOccurrenceRepository.save(updatedOccurrence)

        // Pobieramy dane zdarzenia cyklicznego dla aktywności
        val companyId = securityContext.getCurrentCompanyId()
        val recurringEvent = recurringEventRepository.findById(occurrence.recurringEventId, companyId)
            ?: throw EntityNotFoundException("Recurring event not found: ${occurrence.recurringEventId}")

        // Rejestrujemy aktywność zmiany statusu
        eventActivitySender.onOccurrenceStatusChanged(savedOccurrence, recurringEvent, previousStatus)

        logger.info("Occurrence status updated successfully: {}", occurrenceId)

        return EventOccurrenceResponse.from(savedOccurrence)
    }

    fun prepareVisitCreation(
        occurrenceId: EventOccurrenceId,
        request: ConvertToVisitRequest
    ): CreateVisitRequest {
        logger.info("Preparing visit creation for occurrence: {}", occurrenceId.value)

        val occurrence = eventOccurrenceRepository.findById(occurrenceId)
            ?: throw EntityNotFoundException("Event occurrence not found: ${occurrenceId.value}")

        if (!occurrence.isPlanned()) {
            throw BusinessException("Can only convert planned occurrences to visits")
        }

        return eventToVisitConversionService.buildVisitRequestFromOccurrence(occurrence, request)
    }

    fun markOccurrenceAsConverted(
        occurrenceId: EventOccurrenceId,
        visitId: com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
    ): EventOccurrenceResponse {
        logger.info("Marking occurrence {} as converted to visit {}", occurrenceId.value, visitId.value)

        val occurrence = eventOccurrenceRepository.findById(occurrenceId)
            ?: throw EntityNotFoundException("Event occurrence not found: ${occurrenceId.value}")

        val updatedOccurrence = occurrence.convertToVisit(visitId)
        val savedOccurrence = eventOccurrenceRepository.save(updatedOccurrence)

        logger.info("Occurrence marked as converted successfully: {}", occurrenceId.value)
        return EventOccurrenceResponse.from(savedOccurrence)
    }
}