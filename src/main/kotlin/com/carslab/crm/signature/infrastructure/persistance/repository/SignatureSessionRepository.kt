package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.entity.SignatureSession
import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface SignatureSessionRepository : JpaRepository<SignatureSession, Long> {

    fun findBySessionId(sessionId: String): SignatureSession?

    fun findByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<SignatureSession>

    fun findByWorkstationId(workstationId: UUID): List<SignatureSession>

    fun findByStatus(status: SignatureStatus): List<SignatureSession>

    fun findByStatusAndExpiresAtBefore(status: SignatureStatus, expiresAt: Instant): List<SignatureSession>

    fun countByStatus(status: SignatureStatus): Long

    fun countByStatusAndCreatedAtAfter(status: SignatureStatus, createdAt: Instant): Long

    fun findByTenantIdAndStatus(tenantId: UUID, status: SignatureStatus): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.tenantId = :tenantId 
        AND s.createdAt BETWEEN :startDate AND :endDate
        ORDER BY s.createdAt DESC
    """)
    fun findByTenantIdAndCreatedAtBetween(
        @Param("tenantId") tenantId: UUID,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.workstationId = :workstationId 
        AND s.status = :status
        ORDER BY s.createdAt DESC
    """)
    fun findByWorkstationIdAndStatus(
        @Param("workstationId") workstationId: UUID,
        @Param("status") status: SignatureStatus
    ): List<SignatureSession>

    @Query("""
        SELECT COUNT(s) FROM SignatureSession s 
        WHERE s.tenantId = :tenantId 
        AND s.status = :status
        AND s.createdAt >= :since
    """)
    fun countByTenantIdAndStatusSince(
        @Param("tenantId") tenantId: UUID,
        @Param("status") status: SignatureStatus,
        @Param("since") since: Instant
    ): Long
}