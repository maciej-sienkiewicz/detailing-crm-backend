package com.carslab.crm.production.modules.events.infrastructure.repository

import com.carslab.crm.production.modules.events.domain.models.enums.OccurrenceStatus
import com.carslab.crm.production.modules.events.infrastructure.entity.EventOccurrenceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventOccurrenceJpaRepository : JpaRepository<EventOccurrenceEntity, Long> {

    fun findByRecurringEventId(recurringEventId: Long, pageable: Pageable): Page<EventOccurrenceEntity>

    @Query("""
        SELECT o FROM EventOccurrenceEntity o 
        WHERE o.recurringEventId = :recurringEventId 
        AND o.scheduledDate >= :startDate 
        AND o.scheduledDate <= :endDate
        ORDER BY o.scheduledDate ASC
    """)
    fun findByRecurringEventIdAndDateRange(
        @Param("recurringEventId") recurringEventId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<EventOccurrenceEntity>

    @Query("""
        SELECT o FROM EventOccurrenceEntity o
        JOIN RecurringEventEntity r ON o.recurringEventId = r.id
        WHERE r.companyId = :companyId
        AND o.scheduledDate >= :startDate 
        AND o.scheduledDate <= :endDate
        ORDER BY o.scheduledDate ASC
    """)
    fun findByCompanyIdAndDateRange(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<EventOccurrenceEntity>

    fun findByStatus(status: OccurrenceStatus, pageable: Pageable): Page<EventOccurrenceEntity>

    fun findByActualVisitId(actualVisitId: Long): EventOccurrenceEntity?

    @Modifying
    @Query("DELETE FROM EventOccurrenceEntity o WHERE o.recurringEventId = :recurringEventId")
    fun deleteByRecurringEventId(@Param("recurringEventId") recurringEventId: Long): Int

    fun countByRecurringEventId(recurringEventId: Long): Long

    fun countByRecurringEventIdAndStatus(recurringEventId: Long, status: OccurrenceStatus): Long

    fun existsByRecurringEventIdAndScheduledDate(
        recurringEventId: Long,
        scheduledDate: LocalDateTime
    ): Boolean

    @Query("""
        SELECT o FROM EventOccurrenceEntity o
        JOIN RecurringEventEntity r ON o.recurringEventId = r.id
        WHERE r.companyId = :companyId
        AND o.status IN :statuses
        AND o.scheduledDate >= :fromDate
        ORDER BY o.scheduledDate ASC
    """)
    fun findUpcomingOccurrences(
        @Param("companyId") companyId: Long,
        @Param("statuses") statuses: List<OccurrenceStatus>,
        @Param("fromDate") fromDate: LocalDateTime,
        pageable: Pageable
    ): Page<EventOccurrenceEntity>

    @Query("""
        SELECT o FROM EventOccurrenceEntity o
        WHERE o.recurringEventId = :recurringEventId
        AND o.scheduledDate > :fromDate
        AND o.status = :status
        ORDER BY o.scheduledDate ASC
    """)
    fun findNextOccurrences(
        @Param("recurringEventId") recurringEventId: Long,
        @Param("fromDate") fromDate: LocalDateTime,
        @Param("status") status: OccurrenceStatus,
        pageable: Pageable
    ): Page<EventOccurrenceEntity>
}