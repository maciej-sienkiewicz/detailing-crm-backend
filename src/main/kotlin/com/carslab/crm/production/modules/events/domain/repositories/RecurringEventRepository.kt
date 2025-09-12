package com.carslab.crm.production.modules.events.domain.repositories

import com.carslab.crm.production.modules.events.domain.models.aggregates.RecurringEvent
import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.domain.models.value_objects.RecurringEventId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RecurringEventRepository {
    fun save(recurringEvent: RecurringEvent): RecurringEvent
    fun findById(id: RecurringEventId, companyId: Long): RecurringEvent?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<RecurringEvent>
    fun findByCompanyIdAndType(companyId: Long, type: EventType, pageable: Pageable): Page<RecurringEvent>
    fun findActiveByCompanyId(companyId: Long, pageable: Pageable): Page<RecurringEvent>
    fun existsById(id: RecurringEventId, companyId: Long): Boolean
    fun deleteById(id: RecurringEventId, companyId: Long): Boolean
    fun countByCompanyId(companyId: Long): Long
    fun countByCompanyIdAndType(companyId: Long, type: EventType): Long
}