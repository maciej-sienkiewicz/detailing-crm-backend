package com.carslab.crm.production.modules.events.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.*
import com.carslab.crm.production.modules.events.application.service.command.mapper.EventCommandMapper
import com.carslab.crm.production.modules.events.domain.activity.EventActivitySender
import com.carslab.crm.production.modules.events.domain.factory.RecurringEventFactory
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import com.carslab.crm.production.modules.events.domain.services.RecurrenceCalculationService
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class RecurringEventCommandService(
    private val recurringEventRepository: RecurringEventRepository,
    private val eventOccurrenceRepository: EventOccurrenceRepository,
    private val recurringEventFactory: RecurringEventFactory,
    private val recurrenceCalculationService: RecurrenceCalculationService,
    private val eventCommandMapper: EventCommandMapper,
    private val securityContext: SecurityContext,
    private val eventActivitySender: EventActivitySender
) {
    private val logger = LoggerFactory.getLogger(RecurringEventCommandService::class.java)

    fun createRecurringEvent(request: CreateRecurringEventRequest): RecurringEventResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating recurring event '{}' for company: {}", request.title, companyId)

        val recurringEvent = when (request.type.uppercase()) {
            "SIMPLE_EVENT" -> {
                val recurrencePattern = eventCommandMapper.mapRecurrencePattern(request.recurrencePattern)
                recurringEventFactory.createSimpleEvent(
                    companyId = companyId,
                    title = request.title,
                    description = request.description,
                    recurrencePattern = recurrencePattern
                )
            }
            "RECURRING_VISIT" -> {
                val recurrencePattern = eventCommandMapper.mapRecurrencePattern(request.recurrencePattern)
                val visitTemplate = eventCommandMapper.mapVisitTemplate(request.visitTemplate!!)
                recurringEventFactory.createRecurringVisit(
                    companyId = companyId,
                    title = request.title,
                    description = request.description,
                    recurrencePattern = recurrencePattern,
                    visitTemplate = visitTemplate
                )
            }
            else -> throw IllegalArgumentException("Invalid event type: ${request.type}")
        }

        val savedEvent = recurringEventRepository.save(recurringEvent)
        val occurrencesCount = generateInitialOccurrences(savedEvent)

        // Rejestrujemy aktywność
        eventActivitySender.onRecurringEventCreated(savedEvent, occurrencesCount)

        logger.info("Recurring event created successfully: {}", savedEvent.id)
        return RecurringEventResponse.from(savedEvent)
    }

    fun updateRecurringEvent(eventId: Long, request: UpdateRecurringEventRequest): RecurringEventResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating recurring event: {} for company: {}", eventId, companyId)

        val existingEvent = recurringEventRepository.findById(RecurringEventId.of(eventId), companyId)
            ?: throw EntityNotFoundException("Recurring event not found: $eventId")

        val recurrencePattern = request.recurrencePattern?.let {
            eventCommandMapper.mapRecurrencePattern(it)
        }
        val visitTemplate = request.visitTemplate?.let {
            eventCommandMapper.mapVisitTemplate(it)
        }

        val updatedEvent = recurringEventFactory.updateEvent(
            existingEvent = existingEvent,
            title = request.title,
            description = request.description,
            recurrencePattern = recurrencePattern,
            visitTemplate = visitTemplate
        )

        val savedEvent = recurringEventRepository.save(updatedEvent)

        var regeneratedCount = 0
        if (recurrencePattern != null) {
            regeneratedCount = regenerateOccurrences(savedEvent)
            eventActivitySender.onOccurrencesRegenerated(savedEvent, regeneratedCount)
        }

        // Rejestrujemy aktywność aktualizacji
        eventActivitySender.onRecurringEventUpdated(savedEvent, existingEvent)

        logger.info("Recurring event updated successfully: {}", eventId)
        return RecurringEventResponse.from(savedEvent)
    }

    fun deleteRecurringEvent(eventId: RecurringEventId): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting recurring event: {} for company: {}", eventId.value, companyId)

        val event = recurringEventRepository.findById(eventId, companyId)
            ?: throw EntityNotFoundException("Recurring event not found: ${eventId.value}")

        // Liczymy wystąpienia przed usunięciem
        val occurrencesCount = eventOccurrenceRepository.countByRecurringEventId(eventId).toInt()

        eventOccurrenceRepository.deleteByRecurringEventId(eventId)
        val deleted = recurringEventRepository.deleteById(eventId, companyId)

        if (deleted) {
            // Rejestrujemy aktywność usunięcia
            eventActivitySender.onRecurringEventDeleted(event, occurrencesCount)
            logger.info("Recurring event deleted successfully: {}", eventId.value)
        }
        return deleted
    }

    fun deactivateRecurringEvent(eventId: Long): RecurringEventResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deactivating recurring event: {} for company: {}", eventId, companyId)

        val event = recurringEventRepository.findById(RecurringEventId.of(eventId), companyId)
            ?: throw EntityNotFoundException("Recurring event not found: $eventId")

        val deactivatedEvent = event.changeStatus()
        val savedEvent = recurringEventRepository.save(deactivatedEvent)

        // Rejestrujemy aktywność deaktywacji
        if (!savedEvent.isActive) {
            eventActivitySender.onRecurringEventDeactivated(savedEvent)
        }

        logger.info("Recurring event deactivated successfully: {}", eventId)
        return RecurringEventResponse.from(savedEvent)
    }

    private fun generateInitialOccurrences(event: com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent): Int {
        val fromDate = LocalDateTime.now()
        val toDate = fromDate.plusMonths(6)

        val occurrences = recurrenceCalculationService.generateOccurrences(
            event = event,
            fromDate = fromDate,
            toDate = toDate,
            maxGenerate = 100
        )

        if (occurrences.isNotEmpty()) {
            eventOccurrenceRepository.saveAll(occurrences)
            logger.info("Generated {} initial occurrences for event: {}", occurrences.size, event.id)

            // Rejestrujemy aktywność generowania wystąpień
            eventActivitySender.onOccurrencesGenerated(
                event,
                occurrences.size,
                fromDate.toLocalDate().toString(),
                toDate.toLocalDate().toString()
            )
        }

        return occurrences.size
    }

    private fun regenerateOccurrences(event: com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent): Int {
        eventOccurrenceRepository.deleteByRecurringEventId(event.id!!)
        val newCount = generateInitialOccurrences(event)
        logger.info("Regenerated occurrences for event: {}", event.id)
        return newCount
    }
}