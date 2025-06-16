package com.carslab.crm.signature.infrastructure.persistance.repository

import com.carslab.crm.signature.api.dto.DocumentStatus
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.signature.infrastructure.persistance.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface SignatureDocumentRepository : JpaRepository<SignatureDocument, UUID> {

    fun findByIdAndCompanyId(id: UUID, companyId: Long): SignatureDocument?

    fun findByCompanyIdAndContentHash(companyId: Long, contentHash: String): SignatureDocument?

    fun findByCompanyIdAndStatus(companyId: Long, status: DocumentStatus): List<SignatureDocument>

    @Query("""
        SELECT d FROM SignatureDocument d 
        WHERE d.companyId = :companyId 
        AND d.documentType = :documentType 
        AND d.status = :status
        ORDER BY d.uploadDate DESC
    """)
    fun findByCompanyIdAndDocumentTypeAndStatus(
        @Param("companyId") companyId: Long,
        @Param("documentType") documentType: String,
        @Param("status") status: DocumentStatus
    ): List<SignatureDocument>

    @Query("""
        SELECT d FROM SignatureDocument d 
        WHERE d.companyId = :companyId 
        AND d.uploadDate >= :fromDate 
        AND d.uploadDate <= :toDate
        ORDER BY d.uploadDate DESC
    """)
    fun findByCompanyIdAndUploadDateBetween(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<SignatureDocument>

    @Query("""
        SELECT COUNT(d) FROM SignatureDocument d 
        WHERE d.companyId = :companyId 
        AND d.status = :status
    """)
    fun countByCompanyIdAndStatus(
        @Param("companyId") companyId: Long,
        @Param("status") status: DocumentStatus
    ): Long
}

@Repository
interface DocumentSignatureSessionRepository : JpaRepository<DocumentSignatureSession, UUID> {

    fun findBySessionIdAndCompanyId(sessionId: UUID, companyId: Long): DocumentSignatureSession?
    
    fun findBySessionId(sessionId: UUID): DocumentSignatureSession?

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.documentId = :documentId 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'VIEWING_DOCUMENT', 'SIGNING_IN_PROGRESS')
    """)
    fun findActiveSessionsByDocumentId(@Param("documentId") documentId: UUID): List<DocumentSignatureSession>

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.tabletId = :tabletId 
        AND s.status IN ('SENT_TO_TABLET', 'VIEWING_DOCUMENT', 'SIGNING_IN_PROGRESS')
        ORDER BY s.createdAt DESC
    """)
    fun findActiveSessionsByTabletId(@Param("tabletId") tabletId: UUID): List<DocumentSignatureSession>

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = :status
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndStatus(
        @Param("companyId") companyId: Long,
        @Param("status") status: SignatureSessionStatus
    ): List<DocumentSignatureSession>

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.expiresAt < :now 
        AND s.status IN ('PENDING', 'SENT_TO_TABLET', 'VIEWING_DOCUMENT', 'SIGNING_IN_PROGRESS')
    """)
    fun findExpiredSessions(@Param("now") now: Instant): List<DocumentSignatureSession>

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.createdAt >= :fromDate 
        AND s.createdAt <= :toDate
        ORDER BY s.createdAt DESC
    """)
    fun findByCompanyIdAndCreatedAtBetween(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<DocumentSignatureSession>

    @Query("""
        SELECT COUNT(s) FROM DocumentSignatureSession s 
        WHERE s.companyId = :companyId 
        AND s.status = :status
    """)
    fun countByCompanyIdAndStatus(
        @Param("companyId") companyId: Long,
        @Param("status") status: SignatureSessionStatus
    ): Long

    @Query("""
        SELECT s FROM DocumentSignatureSession s 
        WHERE s.signerName LIKE %:signerName% 
        AND s.companyId = :companyId
        ORDER BY s.createdAt DESC
    """)
    fun findBySignerNameContainingAndCompanyId(
        @Param("signerName") signerName: String,
        @Param("companyId") companyId: Long
    ): List<DocumentSignatureSession>
}

@Repository
interface DocumentPreviewCacheRepository : JpaRepository<DocumentPreviewCache, UUID> {

    fun findByDocumentIdAndPageNumberAndWidth(
        documentId: UUID,
        pageNumber: Int,
        width: Int
    ): DocumentPreviewCache?

    fun findByDocumentId(documentId: UUID): List<DocumentPreviewCache>

    @Query("""
        SELECT p FROM DocumentPreviewCache p 
        WHERE p.lastAccessed < :cutoffDate
        ORDER BY p.lastAccessed ASC
    """)
    fun findStalePreviewsOlderThan(@Param("cutoffDate") cutoffDate: Instant): List<DocumentPreviewCache>

    @Query("""
        SELECT SUM(p.fileSize) FROM DocumentPreviewCache p 
        WHERE p.documentId = :documentId
    """)
    fun getTotalCacheSizeForDocument(@Param("documentId") documentId: UUID): Long?

    @Query("""
        SELECT SUM(p.fileSize) FROM DocumentPreviewCache p
    """)
    fun getTotalCacheSize(): Long?

    fun deleteByDocumentId(documentId: UUID): Int
}

@Repository
interface DocumentSignatureAuditLogRepository : JpaRepository<DocumentSignatureAuditLog, UUID> {

    fun findByDocumentIdOrderByTimestampDesc(documentId: UUID): List<DocumentSignatureAuditLog>

    fun findBySessionIdOrderByTimestampDesc(sessionId: UUID): List<DocumentSignatureAuditLog>

    @Query("""
        SELECT a FROM DocumentSignatureAuditLog a 
        WHERE a.companyId = :companyId 
        AND a.timestamp >= :fromDate 
        AND a.timestamp <= :toDate
        ORDER BY a.timestamp DESC
    """)
    fun findByCompanyIdAndTimestampBetween(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant
    ): List<DocumentSignatureAuditLog>

    @Query("""
        SELECT a FROM DocumentSignatureAuditLog a 
        WHERE a.companyId = :companyId 
        AND a.action = :action
        ORDER BY a.timestamp DESC
    """)
    fun findByCompanyIdAndAction(
        @Param("companyId") companyId: Long,
        @Param("action") action: String
    ): List<DocumentSignatureAuditLog>

    @Query("""
        SELECT a FROM DocumentSignatureAuditLog a 
        WHERE a.performedBy = :performedBy 
        AND a.companyId = :companyId
        ORDER BY a.timestamp DESC
    """)
    fun findByPerformedByAndCompanyId(
        @Param("performedBy") performedBy: String,
        @Param("companyId") companyId: Long
    ): List<DocumentSignatureAuditLog>

    @Query("""
        SELECT COUNT(a) FROM DocumentSignatureAuditLog a 
        WHERE a.companyId = :companyId 
        AND a.action = :action 
        AND a.timestamp >= :fromDate
    """)
    fun countByCompanyIdAndActionAndTimestampAfter(
        @Param("companyId") companyId: Long,
        @Param("action") action: String,
        @Param("fromDate") fromDate: Instant
    ): Long

    @Query("""
        SELECT a FROM DocumentSignatureAuditLog a 
        WHERE a.deviceId = :deviceId 
        AND a.timestamp >= :fromDate
        ORDER BY a.timestamp DESC
    """)
    fun findByDeviceIdAndTimestampAfter(
        @Param("deviceId") deviceId: String,
        @Param("fromDate") fromDate: Instant
    ): List<DocumentSignatureAuditLog>

    @Query("""
        DELETE FROM DocumentSignatureAuditLog a 
        WHERE a.timestamp < :cutoffDate
    """)
    fun deleteOldAuditLogs(@Param("cutoffDate") cutoffDate: Instant): Int
}