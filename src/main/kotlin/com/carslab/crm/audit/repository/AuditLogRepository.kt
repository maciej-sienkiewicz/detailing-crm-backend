package com.carslab.crm.audit.repository

import com.carslab.crm.audit.entity.AuditLog
import com.carslab.crm.audit.entity.AuditSeverity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    fun findByTenantIdAndTimestampBetween(
        tenantId: UUID,
        startTime: Instant,
        endTime: Instant
    ): List<AuditLog>

    fun findBySeverityAndTimestampAfter(
        severity: AuditSeverity,
        timestamp: Instant
    ): List<AuditLog>
}