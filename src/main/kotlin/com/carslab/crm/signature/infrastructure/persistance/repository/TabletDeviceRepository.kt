package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
interface TabletDeviceRepository : JpaRepository<TabletDevice, Long> {

    // Authentication and security
    fun findByDeviceToken(deviceToken: String): TabletDevice?

    fun findByIdAndDeviceToken(id: Long, deviceToken: String): TabletDevice?

    // Company-based queries (replacing tenant-based)
    fun findByCompanyId(companyId: Long): List<TabletDevice>

    fun findByCompanyIdAndStatus(companyId: Long, status: DeviceStatus): List<TabletDevice>

    fun findByCompanyIdAndLocationId(companyId: Long, locationId: Long): List<TabletDevice>

    // Paginated queries for large datasets
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<TabletDevice>

    fun findByCompanyIdAndStatus(companyId: Long, status: DeviceStatus, pageable: Pageable): Page<TabletDevice>

    // Available tablets for assignment
    @Query("""
        SELECT t FROM TabletDevice t 
        WHERE t.companyId = :companyId 
        AND t.locationId = :locationId 
        AND t.status = :status 
        AND t.lastSeen > :lastSeenAfter
        ORDER BY t.lastSeen DESC
    """)
    fun findAvailableTablets(
        @Param("companyId") companyId: Long,
        @Param("locationId") locationId: Long,
        @Param("status") status: DeviceStatus,
        @Param("lastSeenAfter") lastSeenAfter: Instant
    ): List<TabletDevice>

    // Recently active tablets
    @Query("""
        SELECT t FROM TabletDevice t 
        WHERE t.companyId = :companyId 
        AND t.lastSeen > :since
        ORDER BY t.lastSeen DESC
    """)
    fun findRecentlyActiveTablets(
        @Param("companyId") companyId: Long,
        @Param("since") since: Instant
    ): List<TabletDevice>

    // Workstation assignments
    fun findByWorkstationId(workstationId: Long): TabletDevice?

    fun findByCompanyIdAndWorkstationIdIsNotNull(companyId: Long): List<TabletDevice>

    // Status and health monitoring
    fun countByCompanyIdAndStatus(companyId: Long, status: DeviceStatus): Long

    @Query("""
        SELECT COUNT(t) FROM TabletDevice t 
        WHERE t.companyId = :companyId 
        AND t.lastSeen > :since
    """)
    fun countActiveDevicesSince(
        @Param("companyId") companyId: Long,
        @Param("since") since: Instant
    ): Long

    // Update operations
    @Modifying
    @Transactional
    @Query("UPDATE TabletDevice t SET t.lastSeen = :lastSeen, t.updatedAt = :updatedAt WHERE t.id = :deviceId")
    fun updateLastSeen(
        @Param("deviceId") deviceId: Long,
        @Param("lastSeen") lastSeen: Instant,
        @Param("updatedAt") updatedAt: Instant = Instant.now()
    )

    @Modifying
    @Transactional
    @Query("UPDATE TabletDevice t SET t.status = :status, t.updatedAt = :updatedAt WHERE t.id = :deviceId")
    fun updateStatus(
        @Param("deviceId") deviceId: Long,
        @Param("status") status: DeviceStatus,
        @Param("updatedAt") updatedAt: Instant = Instant.now()
    )

    @Modifying
    @Transactional
    @Query("UPDATE TabletDevice t SET t.workstationId = :workstationId, t.updatedAt = :updatedAt WHERE t.id = :deviceId")
    fun updateWorkstationAssignment(
        @Param("deviceId") deviceId: Long,
        @Param("workstationId") workstationId: Long?,
        @Param("updatedAt") updatedAt: Instant = Instant.now()
    )

    // Analytics and reporting
    @Query("""
        SELECT t.status, COUNT(t) 
        FROM TabletDevice t 
        WHERE t.companyId = :companyId 
        GROUP BY t.status
    """)
    fun getStatusDistribution(@Param("companyId") companyId: Long): List<Array<Any>>

    @Query("""
        SELECT t.locationId, COUNT(t) 
        FROM TabletDevice t 
        WHERE t.companyId = :companyId 
        AND t.status = 'ACTIVE'
        GROUP BY t.locationId
    """)
    fun getActiveDevicesByLocation(@Param("companyId") companyId: Long): List<Array<Any>>

    // Cleanup and maintenance
    @Query("""
        SELECT t FROM TabletDevice t 
        WHERE t.status = 'INACTIVE' 
        AND t.updatedAt < :cutoffDate
    """)
    fun findInactiveDevicesOlderThan(@Param("cutoffDate") cutoffDate: Instant): List<TabletDevice>
}