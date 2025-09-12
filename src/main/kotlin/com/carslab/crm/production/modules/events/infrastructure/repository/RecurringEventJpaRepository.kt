package com.carslab.crm.production.modules.events.infrastructure.repository

import com.carslab.crm.production.modules.events.domain.models.enums.EventType
import com.carslab.crm.production.modules.events.infrastructure.entity.RecurringEventEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecurringEventJpaRepository : JpaRepository<RecurringEventEntity, Long> {

    fun findByIdAndCompanyId(id: Long, companyId: Long): RecurringEventEntity?

    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<RecurringEventEntity>

    fun findByCompanyIdAndEventType(
        companyId: Long,
        eventType: EventType,
        pageable: Pageable
    ): Page<RecurringEventEntity>

    fun findByCompanyIdAndIsActive(
        companyId: Long,
        isActive: Boolean,
        pageable: Pageable
    ): Page<RecurringEventEntity>

    fun existsByIdAndCompanyId(id: Long, companyId: Long): Boolean

    @Modifying
    @Query("DELETE FROM RecurringEventEntity r WHERE r.id = :id AND r.companyId = :companyId")
    fun deleteByIdAndCompanyId(@Param("id") id: Long, @Param("companyId") companyId: Long): Int

    fun countByCompanyId(companyId: Long): Long

    fun countByCompanyIdAndEventType(companyId: Long, eventType: EventType): Long

    @Query("""
        SELECT r FROM RecurringEventEntity r 
        WHERE r.companyId = :companyId 
        AND r.isActive = true 
        AND (r.endDate IS NULL OR r.endDate > CURRENT_TIMESTAMP)
        ORDER BY r.updatedAt DESC
    """)
    fun findActiveEvents(@Param("companyId") companyId: Long, pageable: Pageable): Page<RecurringEventEntity>
}