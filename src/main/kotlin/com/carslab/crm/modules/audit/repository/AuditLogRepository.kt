package com.carslab.crm.audit.repository

import com.carslab.crm.audit.entity.AuditLog
import com.carslab.crm.audit.entity.AuditSeverity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    fun findByUserIdAndTimestampAfter(
        userId: UUID,
        timestamp: Instant
    ): List<AuditLog>

    fun findByActionAndTimestampAfter(
        action: String,
        timestamp: Instant
    ): List<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.tenantId = :tenantId 
        AND a.severity = :severity 
        AND a.timestamp BETWEEN :startTime AND :endTime
        ORDER BY a.timestamp DESC
    """)
    fun findByTenantIdAndSeverityBetween(
        @Param("tenantId") tenantId: UUID,
        @Param("severity") severity: AuditSeverity,
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.resource = :resource 
        AND a.resourceId = :resourceId 
        ORDER BY a.timestamp DESC
    """)
    fun findByResourceAndResourceId(
        @Param("resource") resource: String,
        @Param("resourceId") resourceId: String
    ): List<AuditLog>

    @Query("""
        SELECT COUNT(a) FROM AuditLog a 
        WHERE a.tenantId = :tenantId 
        AND a.severity = :severity 
        AND a.timestamp > :since
    """)
    fun countByTenantIdAndSeveritySince(
        @Param("tenantId") tenantId: UUID,
        @Param("severity") severity: AuditSeverity,
        @Param("since") since: Instant
    ): Long

    // Security-related queries
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.action LIKE '%SECURITY%' 
        OR a.severity = 'CRITICAL'
        ORDER BY a.timestamp DESC
    """)
    fun findSecurityEvents(): List<AuditLog>

    // Cleanup method for old audit logs
    fun deleteByTimestampBefore(timestamp: Instant): Long
}