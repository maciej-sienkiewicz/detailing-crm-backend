package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureSession
import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SignatureSessionRepository : JpaRepository<SignatureSession, Long> {

    // Primary lookups
    fun findBySessionId(sessionId: String): SignatureSession?

    // Company-based queries (replacing tenant-based)
    fun findByCompanyIdOrderByCreatedAtDesc(companyId: Long): List<SignatureSession>

    fun findByCompanyIdOrderByCreatedAtDesc(companyId: Long, pageable: Pageable): Page<SignatureSession>

    fun findByWorkstationId(workstationId: Long): List<SignatureSession>

    fun findByAssignedTabletId(assignedTabletId: Long): List<SignatureSession>

    // Status-based queries
    fun findByStatus(status: SignatureStatus): List<SignatureSession>

    fun findByCompanyIdAndStatus(companyId: Long, status: SignatureStatus): List<SignatureSession>

    fun findByStatusAndExpiresAtBefore(status: SignatureStatus, expiresAt: Instant): List<SignatureSession>

    // Time-based queries
    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt BETWEEN :startDate AND :endDate
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndCreatedAtBetween(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt BETWEEN :startDate AND :endDate
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndCreatedAtBetween(
        @Param("companyId") companyId: Long,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
        pageable: Pageable
    ): Page<SignatureSession>

    // Customer and vehicle searches
    fun findByCompanyIdAndCustomerNameContainingIgnoreCase(
        companyId: Long,
        customerName: String
    ): List<SignatureSession>

    fun findByCompanyIdAndVehicleLicensePlate(
        companyId: Long,
        licensePlate: String
    ): List<SignatureSession>

    // Counting and analytics
    fun countByStatus(status: SignatureStatus): Long

    fun countByCompanyIdAndStatus(companyId: Long, status: SignatureStatus): Long

    fun countByStatusAndCreatedAtAfter(status: SignatureStatus, createdAt: Instant): Long

    fun countByCompanyIdAndStatusAndCreatedAtAfter(
        companyId: Long,
        status: SignatureStatus,
        createdAt: Instant
    ): Long

    @Query("""
        SELECT COUNT(s) FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = :status
        AND s.createdAt >= :since
    """)
    fun countByCompanyIdAndStatusSince(
        @Param("companyId") companyId: Long,
        @Param("status") status: SignatureStatus,
        @Param("since") since: Instant
    ): Long

    // Performance and monitoring queries
    @Query("""
        SELECT s.status, COUNT(s) 
        FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt >= :since
        GROUP BY s.status
    """)
    fun getStatusDistributionSince(
        @Param("companyId") companyId: Long,
        @Param("since") since: Instant
    ): List<Array<Any>>

    @Query("""
        SELECT AVG(s.signatureDurationSeconds) 
        FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = 'COMPLETED'
        AND s.signatureDurationSeconds IS NOT NULL
        AND s.createdAt >= :since
    """)
    fun getAverageSignatureDurationSince(
        @Param("companyId") companyId: Long,
        @Param("since") since: Instant
    ): Double?

    // Cleanup queries
    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.status IN ('COMPLETED', 'EXPIRED', 'CANCELLED')
        AND s.createdAt < :cutoffDate
    """)
    fun findOldCompletedSessions(@Param("cutoffDate") cutoffDate: Instant): List<SignatureSession>
}