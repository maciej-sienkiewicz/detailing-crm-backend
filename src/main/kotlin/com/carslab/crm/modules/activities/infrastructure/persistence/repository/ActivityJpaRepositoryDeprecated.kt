// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/persistence/repository/ActivityJpaRepository.kt
package com.carslab.crm.modules.activities.infrastructure.persistence.repository

import com.carslab.crm.modules.activities.infrastructure.persistence.entity.ActivityEntityDeprecated
import com.carslab.crm.modules.activities.application.queries.models.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityJpaRepositoryDeprecated : JpaRepository<ActivityEntityDeprecated, Long>, JpaSpecificationExecutor<ActivityEntityDeprecated> {

    /**
     * Find activity by activityId and companyId
     */
    fun findByActivityIdAndCompanyId(activityId: String, companyId: Long): ActivityEntityDeprecated?

    /**
     * Find activities by company with pagination
     */
    fun findByCompanyIdOrderByTimestampDesc(companyId: Long, pageable: Pageable): Page<ActivityEntityDeprecated>

    /**
     * Find activities by entity
     */
    fun findByCompanyIdAndEntityTypeAndEntityIdOrderByTimestampDesc(
        companyId: Long,
        entityType: EntityType,
        entityId: String,
        pageable: Pageable
    ): Page<ActivityEntityDeprecated>

    /**
     * Find recent activities for company
     */
    @Query(
        """
        SELECT a FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        ORDER BY a.timestamp DESC
    """
    )
    fun findRecentActivities(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): List<ActivityEntityDeprecated>

    /**
     * Count activities by category for company
     */
    fun countByCompanyIdAndCategory(companyId: Long, category: ActivityCategory): Long

    /**
     * Count activities by status for company
     */
    fun countByCompanyIdAndStatus(companyId: Long, status: ActivityStatus): Long

    /**
     * Daily statistics query
     */
    @Query(
        """
        SELECT 
            DATE(a.timestamp) as date,
            SUM(CASE WHEN a.category = 'APPOINTMENT' THEN 1 ELSE 0 END) as appointmentsScheduled,
            SUM(CASE WHEN a.category = 'PROTOCOL' THEN 1 ELSE 0 END) as protocolsCompleted,
            SUM(CASE WHEN a.category = 'VEHICLE' THEN 1 ELSE 0 END) as vehiclesServiced,
            SUM(CASE WHEN a.category = 'CLIENT' THEN 1 ELSE 0 END) as newClients,
            SUM(CASE WHEN a.category = 'COMMENT' THEN 1 ELSE 0 END) as commentsAdded,
            COUNT(*) as totalActivities
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND DATE(a.timestamp) = :date
        GROUP BY DATE(a.timestamp)
    """
    )
    fun getDailySummary(
        @Param("companyId") companyId: Long,
        @Param("date") date: java.time.LocalDate
    ): List<Array<Any>>?

    /**
     * Category breakdown for analytics
     */
    @Query(
        """
        SELECT a.category, COUNT(*) 
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        GROUP BY a.category
    """
    )
    fun getCategoryBreakdown(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * User breakdown for analytics
     */
    @Query(
        """
        SELECT a.userId, a.userName, COUNT(*) 
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        AND a.userId IS NOT NULL
        GROUP BY a.userId, a.userName
        ORDER BY COUNT(*) DESC
    """
    )
    fun getUserBreakdown(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Status breakdown for analytics
     */
    @Query(
        """
        SELECT COALESCE(a.status, 'NONE'), COUNT(*) 
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        GROUP BY a.status
    """
    )
    fun getStatusBreakdown(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Daily trends for analytics
     */
    @Query(
        """
        SELECT 
            DATE(a.timestamp) as date,
            a.category,
            COUNT(*) as count
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        GROUP BY DATE(a.timestamp), a.category
        ORDER BY DATE(a.timestamp)
    """
    )
    fun getDailyTrends(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Entity statistics
     */
    @Query(
        """
        SELECT 
            a.entityType,
            a.entityId,
            COUNT(*) as activityCount,
            MAX(a.timestamp) as lastActivity
        FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp BETWEEN :startDate AND :endDate
        AND a.entityType IS NOT NULL
        AND a.entityId IS NOT NULL
        GROUP BY a.entityType, a.entityId
        ORDER BY COUNT(*) DESC
    """
    )
    fun getEntityStats(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Delete old activities (for cleanup)
     */
    @Query(
        """
        DELETE FROM ActivityEntityDeprecated a 
        WHERE a.companyId = :companyId 
        AND a.timestamp < :cutoffDate
    """
    )
    fun deleteOldActivities(
        @Param("companyId") companyId: Long,
        @Param("cutoffDate") cutoffDate: LocalDateTime
    ): Int

    /**
     * Check if activity exists
     */
    fun existsByActivityIdAndCompanyId(activityId: String, companyId: Long): Boolean
}