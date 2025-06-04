package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.infrastructure.persistance.entity.PairingCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface PairingCodeRepository : JpaRepository<PairingCode, String> {

    // Active pairing codes
    fun findByCodeAndExpiresAtAfter(code: String, expiresAt: Instant): PairingCode?

    fun findByCompanyIdAndExpiresAtAfter(companyId: Long, expiresAt: Instant): List<PairingCode>

    // Cleanup operations
    @Modifying
    @Transactional
    @Query("DELETE FROM PairingCode p WHERE p.expiresAt < :expiresAt")
    fun deleteExpiredCodes(@Param("expiresAt") expiresAt: Instant): Int

    // Usage tracking
    @Modifying
    @Transactional
    @Query("""
        UPDATE PairingCode p 
        SET p.usedAt = :usedAt, p.usedByTabletId = :tabletId 
        WHERE p.code = :code
    """)
    fun markAsUsed(
        @Param("code") code: String,
        @Param("usedAt") usedAt: Instant,
        @Param("tabletId") tabletId: Long
    )

    // Analytics
    fun countByCompanyIdAndCreatedAtAfter(companyId: Long, createdAt: Instant): Long

    @Query("""
        SELECT COUNT(p) FROM PairingCode p 
        WHERE p.companyId = :companyId 
        AND p.usedAt IS NOT NULL
        AND p.createdAt >= :since
    """)
    fun countUsedCodesSince(
        @Param("companyId") companyId: Long,
        @Param("since") since: Instant
    ): Long
}