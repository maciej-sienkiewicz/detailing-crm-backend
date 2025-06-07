// src/main/kotlin/com/carslab/crm/signature/infrastructure/persistance/repository/SignatureSessionRepository.kt
package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.api.dto.SignatureStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.SignatureSession
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface SignatureSessionRepository : JpaRepository<SignatureSession, UUID> {

    fun findBySessionIdAndCompanyId(sessionId: UUID, companyId: Long): SignatureSession?

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.workstationId = :workstationId 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'IN_PROGRESS')
        ORDER BY s.createdAt DESC
    """)
    fun findActiveSessionsByWorkstationId(@Param("workstationId") workstationId: UUID): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.tabletId = :tabletId 
        AND s.status IN ('SENT_TO_TABLET', 'IN_PROGRESS')
        ORDER BY s.createdAt DESC
    """)
    fun findActiveSessionsByTabletId(@Param("tabletId") tabletId: UUID): List<SignatureSession>

    fun findByCompanyIdAndStatus(
        companyId: Long,
        status: SignatureStatus,
        pageable: Pageable
    ): List<SignatureSession>

    fun findByCompanyIdOrderByCreatedAtDesc(
        companyId: Long,
        pageable: Pageable
    ): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.expiresAt < :now 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'IN_PROGRESS')
    """)
    fun findExpiredSessions(@Param("now") now: Instant): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt >= :fromDate 
        AND s.createdAt <= :toDate
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndCreatedAtBetween(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<SignatureSession>

    @Query("""
        SELECT COUNT(s) FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = :status
    """)
    fun countByCompanyIdAndStatus(
        @Param("companyId") companyId: Long,
        @Param("status") status: SignatureStatus
    ): Long

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.customerName LIKE %:customerName% 
        AND s.companyId = :companyId
        ORDER BY s.createdAt DESC
    """)
    fun findByCustomerNameContainingAndCompanyId(
        @Param("customerName") customerName: String,
        @Param("companyId") companyId: Long
    ): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = 'COMPLETED'
        AND s.signedAt >= :fromDate
        ORDER BY s.signedAt DESC
    """)
    fun findCompletedSessionsSince(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant
    ): List<SignatureSession>

    @Query("""
        SELECT s FROM SignatureSession s 
        WHERE s.tabletId = :tabletId 
        AND s.createdAt >= :fromDate
        ORDER BY s.createdAt DESC
    """)
    fun findByTabletIdAndCreatedAtAfter(
        @Param("tabletId") tabletId: UUID,
        @Param("fromDate") fromDate: Instant
    ): List<SignatureSession>
}