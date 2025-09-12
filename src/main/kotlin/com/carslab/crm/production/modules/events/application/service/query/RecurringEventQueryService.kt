// src/main/kotlin/com/carslab/crm/production/modules/events/application/service/query/RecurringEventQueryService.kt
package com.carslab.crm.production.modules.events.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.events.application.dto.RecurringEventResponse
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RecurringEventQueryService(
    private val recurringEventRepository: RecurringEventRepository,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(RecurringEventQueryService::class.java)

    fun getRecurringEvent(eventId: Long): RecurringEventResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching recurring event: {} for company: {}", eventId, companyId)

        val event = recurringEventRepository.findById(RecurringEventId.of(eventId), companyId)
            ?: throw EntityNotFoundException("Recurring event not found: $eventId")

        return RecurringEventResponse.from(event)
    }

    fun getRecurringEvents(pageable: Pageable): Page<RecurringEventResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching recurring events for company: {}", companyId)

        val events = recurringEventRepository.findByCompanyId(companyId, pageable)
        return events.map { RecurringEventResponse.from(it) }
    }

    fun getRecurringEventsByType(type: String, pageable: Pageable): Page<RecurringEventResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val eventType = try {
            EventType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid event type: $type")
        }

        logger.debug("Fetching recurring events of type {} for company: {}", type, companyId)

        val events = recurringEventRepository.findByCompanyIdAndType(companyId, eventType, pageable)
        return events.map { RecurringEventResponse.from(it) }
    }

    fun getActiveRecurringEvents(pageable: Pageable): Page<RecurringEventResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching active recurring events for company: {}", companyId)

        val events = recurringEventRepository.findActiveByCompanyId(companyId, pageable)
        return events.map { RecurringEventResponse.from(it) }
    }

    fun getRecurringEventsCount(): Long {
        val companyId = securityContext.getCurrentCompanyId()
        return recurringEventRepository.countByCompanyId(companyId)
    }

    fun getRecurringEventsCountByType(type: String): Long {
        val companyId = securityContext.getCurrentCompanyId()
        val eventType = try {
            EventType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid event type: $type")
        }
        return recurringEventRepository.countByCompanyIdAndType(companyId, eventType)
    }
}