package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.SimpleSignatureSession
import com.carslab.crm.signature.api.dto.SimpleSignatureStatus
import com.carslab.crm.signature.api.dto.SimpleSignatureType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface SimpleSignatureSessionRepository : JpaRepository<SimpleSignatureSession, UUID> {

    fun findBySessionIdAndCompanyId(sessionId: UUID, companyId: Long): SimpleSignatureSession?

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.tabletId = :tabletId 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'IN_PROGRESS')
        ORDER BY s.createdAt DESC
    """)
    fun findActiveSessionsByTabletId(@Param("tabletId") tabletId: UUID): List<SimpleSignatureSession>

    fun findByCompanyIdAndStatus(
        companyId: Long,
        status: SimpleSignatureStatus,
        pageable: Pageable
    ): List<SimpleSignatureSession>

    fun findByCompanyIdOrderByCreatedAtDesc(
        companyId: Long,
        pageable: Pageable
    ): List<SimpleSignatureSession>

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.expiresAt < :now 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'IN_PROGRESS')
    """)
    fun findExpiredSessions(@Param("now") now: Instant): List<SimpleSignatureSession>

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt >= :fromDate 
        AND s.createdAt <= :toDate
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndCreatedAtBetween(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<SimpleSignatureSession>

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.signerName LIKE %:signerName% 
        AND s.companyId = :companyId
        ORDER BY s.createdAt DESC
    """)
    fun findBySignerNameContainingAndCompanyId(
        @Param("signerName") signerName: String,
        @Param("companyId") companyId: Long
    ): List<SimpleSignatureSession>

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.externalReference = :externalReference 
        AND s.companyId = :companyId
        ORDER BY s.createdAt DESC
    """)
    fun findByExternalReferenceAndCompanyId(
        @Param("externalReference") externalReference: String,
        @Param("companyId") companyId: Long
    ): List<SimpleSignatureSession>

    @Query("""
        SELECT s FROM SimpleSignatureSession s 
        WHERE s.signatureType = :signatureType 
        AND s.companyId = :companyId
        ORDER BY s.createdAt DESC
    """)
    fun findBySignatureTypeAndCompanyId(
        @Param("signatureType") signatureType: SimpleSignatureType,
        @Param("companyId") companyId: Long
    ): List<SimpleSignatureSession>

    @Query("""
        SELECT COUNT(s) FROM SimpleSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = :status
    """)
    fun countByCompanyIdAndStatus(
        @Param("companyId") companyId: Long,
        @Param("status") status: SimpleSignatureStatus
    ): Long

    @Query("""
        SELECT COUNT(s) FROM SimpleSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.signatureType = :signatureType 
        AND s.createdAt >= :fromDate
    """)
    fun countByCompanyIdAndSignatureTypeAndCreatedAtAfter(
        @Param("companyId") companyId: Long,
        @Param("signatureType") signatureType: SimpleSignatureType,
        @Param("fromDate") fromDate: Instant
    ): Long
}