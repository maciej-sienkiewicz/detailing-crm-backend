package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface TabletDeviceRepository : JpaRepository<TabletDevice, UUID> {

    fun findByDeviceToken(deviceToken: String): TabletDevice?

    fun findByIdAndDeviceToken(id: UUID, deviceToken: String): TabletDevice?

    fun findByTenantId(tenantId: UUID): List<TabletDevice>

    fun findByTenantIdAndLocationId(tenantId: UUID, locationId: UUID): List<TabletDevice>

    @Query("""
        SELECT t FROM TabletDevice t 
        WHERE t.tenantId = :tenantId 
        AND t.locationId = :locationId 
        AND t.status = :status 
        AND t.lastSeen > :lastSeenAfter
        ORDER BY t.lastSeen DESC
    """)
    fun findAvailableTablets(
        tenantId: UUID,
        locationId: UUID,
        status: DeviceStatus,
        lastSeenAfter: Instant
    ): List<TabletDevice>

    @Modifying
    @Query("UPDATE TabletDevice t SET t.lastSeen = :lastSeen WHERE t.id = :deviceId")
    fun updateLastSeen(deviceId: UUID, lastSeen: Instant)
}
