package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.TabletDevice
import com.carslab.crm.signature.infrastructure.persistance.entity.DeviceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
interface TabletDeviceRepository : JpaRepository<TabletDevice, UUID> {

    fun findByIdAndDeviceToken(id: UUID, deviceToken: String): TabletDevice?

    fun findByCompanyId(companyId: Long): List<TabletDevice>
    
    @Modifying
    @Transactional
    @Query("UPDATE TabletDevice t SET t.lastSeen = :lastSeen WHERE t.id = :deviceId")
    fun updateLastSeen(deviceId: UUID, lastSeen: Instant)
}