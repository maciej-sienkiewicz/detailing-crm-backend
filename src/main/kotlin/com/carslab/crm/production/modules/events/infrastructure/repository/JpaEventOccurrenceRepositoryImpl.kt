package com.carslab.crm.production.modules.events.infrastructure.repository

import com.carslab.crm.production.modules.events.domain.models.aggregates.EventOccurrence
import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.domain.models.value_objects.EventOccurrenceId
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.repositories.EventOccurrenceRepository
import com.carslab.crm.production.modules.events.infrastructure.entity.EventOccurrenceEntity
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class JpaEventOccurrenceRepositoryImpl(
    private val eventOccurrenceJpaRepository: EventOccurrenceJpaRepository
) : EventOccurrenceRepository {

    override fun save(eventOccurrence: EventOccurrence): EventOccurrence {
        val entity = EventOccurrenceEntity.fromDomain(eventOccurrence)
        val savedEntity = eventOccurrenceJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun saveAll(eventOccurrences: List<EventOccurrence>): List<EventOccurrence> {
        val entities = eventOccurrences.map { EventOccurrenceEntity.fromDomain(it) }
        val savedEntities = eventOccurrenceJpaRepository.saveAll(entities)
        return savedEntities.map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findById(id: EventOccurrenceId): EventOccurrence? {
        return eventOccurrenceJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByRecurringEventId(
        recurringEventId: RecurringEventId,
        pageable: Pageable
    ): Page<EventOccurrence> {
        return eventOccurrenceJpaRepository.findByRecurringEventId(recurringEventId.value, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByRecurringEventIdAndDateRange(
        recurringEventId: RecurringEventId,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventOccurrence> {
        return eventOccurrenceJpaRepository.findByRecurringEventIdAndDateRange(
            recurringEventId.value, startDate, endDate
        ).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndDateRange(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<EventOccurrence> {
        return eventOccurrenceJpaRepository.findByCompanyIdAndDateRange(
            companyId, startDate, endDate, pageable
        ).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByStatus(status: OccurrenceStatus, pageable: Pageable): Page<EventOccurrence> {
        return eventOccurrenceJpaRepository.findByStatus(status, pageable)
            .map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByVisitId(visitId: VisitId): EventOccurrence? {
        return eventOccurrenceJpaRepository.findByActualVisitId(visitId.value)
            ?.toDomain()
    }

    override fun deleteById(id: EventOccurrenceId): Boolean {
        return if (eventOccurrenceJpaRepository.existsById(id.value)) {
            eventOccurrenceJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun deleteByRecurringEventId(recurringEventId: RecurringEventId): Int {
        return eventOccurrenceJpaRepository.deleteByRecurringEventId(recurringEventId.value)
    }

    @Transactional(readOnly = true)
    override fun countByRecurringEventId(recurringEventId: RecurringEventId): Long {
        return eventOccurrenceJpaRepository.countByRecurringEventId(recurringEventId.value)
    }

    @Transactional(readOnly = true)
    override fun countByRecurringEventIdAndStatus(
        recurringEventId: RecurringEventId,
        status: OccurrenceStatus
    ): Long {
        return eventOccurrenceJpaRepository.countByRecurringEventIdAndStatus(
            recurringEventId.value, status
        )
    }

    @Transactional(readOnly = true)
    override fun existsByRecurringEventIdAndScheduledDate(
        recurringEventId: RecurringEventId,
        scheduledDate: LocalDateTime
    ): Boolean {
        return eventOccurrenceJpaRepository.existsByRecurringEventIdAndScheduledDate(
            recurringEventId.value, scheduledDate
        )
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndDateRangeWithoutPagination(
        companyId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventOccurrence> {
        return eventOccurrenceJpaRepository.findByCompanyIdAndDateRangeWithoutPagination(
            companyId, startDate, endDate
        ).map { it.toDomain() }
    }
}