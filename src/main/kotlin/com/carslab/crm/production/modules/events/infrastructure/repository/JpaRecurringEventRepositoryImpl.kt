package com.carslab.crm.production.modules.events.infrastructure.repository

import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import com.carslab.crm.production.modules.events.domain.repositories.RecurringEventRepository
import com.carslab.crm.production.modules.events.infrastructure.entity.RecurringEventEntity
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaRecurringEventRepositoryImpl(
    private val recurringEventJpaRepository: RecurringEventJpaRepository,
    private val objectMapper: ObjectMapper
) : RecurringEventRepository {

    override fun save(recurringEvent: RecurringEvent): RecurringEvent {
        val entity = RecurringEventEntity.fromDomain(recurringEvent, objectMapper)
        val savedEntity = recurringEventJpaRepository.save(entity)
        return savedEntity.toDomain(objectMapper)
    }

    @Transactional(readOnly = true)
    override fun findById(id: RecurringEventId, companyId: Long): RecurringEvent? {
        return recurringEventJpaRepository.findByIdAndCompanyId(id.value, companyId)
            ?.toDomain(objectMapper)
    }

    @Transactional(readOnly = true)
    override fun findByCompanyId(companyId: Long, pageable: Pageable): Page<RecurringEvent> {
        return recurringEventJpaRepository.findByCompanyId(companyId, pageable)
            .map { it.toDomain(objectMapper) }
    }

    @Transactional(readOnly = true)
    override fun findByCompanyIdAndType(
        companyId: Long,
        type: EventType,
        pageable: Pageable
    ): Page<RecurringEvent> {
        return recurringEventJpaRepository.findByCompanyIdAndEventType(companyId, type, pageable)
            .map { it.toDomain(objectMapper) }
    }

    @Transactional(readOnly = true)
    override fun findActiveByCompanyId(companyId: Long, pageable: Pageable): Page<RecurringEvent> {
        return recurringEventJpaRepository.findActiveEvents(companyId, pageable)
            .map { it.toDomain(objectMapper) }
    }

    @Transactional(readOnly = true)
    override fun existsById(id: RecurringEventId, companyId: Long): Boolean {
        return recurringEventJpaRepository.existsByIdAndCompanyId(id.value, companyId)
    }

    override fun deleteById(id: RecurringEventId, companyId: Long): Boolean {
        return recurringEventJpaRepository.deleteByIdAndCompanyId(id.value, companyId) > 0
    }

    @Transactional(readOnly = true)
    override fun countByCompanyId(companyId: Long): Long {
        return recurringEventJpaRepository.countByCompanyId(companyId)
    }

    @Transactional(readOnly = true)
    override fun countByCompanyIdAndType(companyId: Long, type: EventType): Long {
        return recurringEventJpaRepository.countByCompanyIdAndEventType(companyId, type)
    }
}
