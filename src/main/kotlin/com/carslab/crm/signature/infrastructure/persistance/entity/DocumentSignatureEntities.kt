package com.carslab.crm.signature.infrastructure.persistance.entity

import com.carslab.crm.signature.api.dto.DocumentStatus
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "signature_documents",
    indexes = [
        Index(name = "idx_signature_documents_company_id", columnList = "companyId"),
        Index(name = "idx_signature_documents_status", columnList = "status"),
        Index(name = "idx_signature_documents_type", columnList = "documentType"),
        Index(name = "idx_signature_documents_upload_date", columnList = "uploadDate")
    ]
)
data class SignatureDocument(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, length = 100)
    val documentType: String,

    @Column(nullable = false, length = 255)
    val fileName: String,

    @Column(nullable = false, length = 500)
    val filePath: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val pageCount: Int,

    @Column(nullable = false, length = 64)
    val contentHash: String,

    @Column(nullable = false, length = 50)
    val mimeType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: DocumentStatus = DocumentStatus.UPLOADED,

    @Column(nullable = false)
    val uploadDate: Instant = Instant.now(),

    @Column(nullable = false, length = 100)
    val uploadedBy: String,

    /**
     * Business-agnostic metadata as JSON
     * Can contain any domain-specific information
     */
    @Column(columnDefinition = "TEXT")
    val metadata: String? = null,

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun updateStatus(newStatus: DocumentStatus): SignatureDocument {
        return this.copy(status = newStatus, updatedAt = Instant.now())
    }
}

@Entity
@Table(
    name = "document_signature_sessions",
    indexes = [
        Index(name = "idx_doc_sig_sessions_company_id", columnList = "companyId"),
        Index(name = "idx_doc_sig_sessions_status", columnList = "status"),
        Index(name = "idx_doc_sig_sessions_expires_at", columnList = "expiresAt"),
        Index(name = "idx_doc_sig_sessions_document_id", columnList = "documentId"),
        Index(name = "idx_doc_sig_sessions_tablet_id", columnList = "tabletId")
    ]
)
data class DocumentSignatureSession(
    @Id
    val sessionId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val documentId: UUID,

    @Column(nullable = false)
    val tabletId: UUID,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val signerName: String,

    @Column(nullable = false, length = 200)
    val signatureTitle: String,

    @Column(columnDefinition = "TEXT")
    val instructions: String? = null,

    /**
     * Business context as JSON - domain agnostic
     * Can contain vehicle info, customer details, order info, etc.
     */
    @Column(columnDefinition = "TEXT")
    val businessContext: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: SignatureSessionStatus = SignatureSessionStatus.PENDING,

    @Column(nullable = false, length = 100)
    val createdBy: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val expiresAt: Instant,

    val signedAt: Instant? = null,

    @Column(length = 500)
    val signatureImagePath: String? = null,

    @Column(length = 500)
    val signedDocumentPath: String? = null,

    /**
     * Signature field definitions as JSON
     */
    @Column(columnDefinition = "TEXT")
    val signatureFields: String? = null,

    /**
     * Actual signature placement as JSON
     */
    @Column(columnDefinition = "TEXT")
    val signaturePlacement: String? = null,

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Column(length = 500)
    val errorMessage: String? = null,

    @Column(length = 200)
    val cancelledBy: String? = null,

    val cancelledAt: Instant? = null,

    @Column(length = 500)
    val cancellationReason: String? = null
) {
    fun updateStatus(newStatus: SignatureSessionStatus, errorMessage: String? = null): DocumentSignatureSession {
        return this.copy(
            status = newStatus,
            updatedAt = Instant.now(),
            errorMessage = errorMessage
        )
    }

    fun markAsSigned(signatureImagePath: String, signaturePlacement: String? = null): DocumentSignatureSession {
        return this.copy(
            status = SignatureSessionStatus.COMPLETED,
            signedAt = Instant.now(),
            signatureImagePath = signatureImagePath,
            signaturePlacement = signaturePlacement,
            updatedAt = Instant.now()
        )
    }

    fun cancel(cancelledBy: String, reason: String? = null): DocumentSignatureSession {
        return this.copy(
            status = SignatureSessionStatus.CANCELLED,
            cancelledBy = cancelledBy,
            cancelledAt = Instant.now(),
            cancellationReason = reason,
            updatedAt = Instant.now()
        )
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    fun canBeSigned(): Boolean = status == SignatureSessionStatus.SENT_TO_TABLET && !isExpired()
}

@Entity
@Table(
    name = "document_preview_cache",
    indexes = [
        Index(name = "idx_doc_preview_cache_document_id", columnList = "documentId"),
        Index(name = "idx_doc_preview_cache_last_accessed", columnList = "lastAccessed")
    ]
)
data class DocumentPreviewCache(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val documentId: UUID,

    @Column(nullable = false)
    val pageNumber: Int,

    @Column(nullable = false)
    val width: Int,

    @Column(nullable = false, length = 500)
    val previewPath: String,

    @Column(nullable = false, length = 64)
    val previewHash: String,

    @Column(nullable = false)
    val previewWidth: Int,

    @Column(nullable = false)
    val previewHeight: Int,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val lastAccessed: Instant = Instant.now(),

    @Column(nullable = false)
    val fileSize: Long
) {
    fun updateLastAccessed(): DocumentPreviewCache {
        return this.copy(lastAccessed = Instant.now())
    }
}

/**
 * Comprehensive audit log for document signature operations
 */
@Entity
@Table(
    name = "document_signature_audit_log",
    indexes = [
        Index(name = "idx_doc_sig_audit_document_id", columnList = "documentId"),
        Index(name = "idx_doc_sig_audit_session_id", columnList = "sessionId"),
        Index(name = "idx_doc_sig_audit_company_id", columnList = "companyId"),
        Index(name = "idx_doc_sig_audit_timestamp", columnList = "timestamp"),
        Index(name = "idx_doc_sig_audit_action", columnList = "action")
    ]
)
data class DocumentSignatureAuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val documentId: UUID,

    val sessionId: UUID? = null,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 50)
    val action: String, // UPLOADED, SENT_FOR_SIGNATURE, DOCUMENT_VIEWED, SIGNATURE_STARTED, SIGNED, CANCELLED, DOWNLOADED, etc.

    @Column(nullable = false, length = 100)
    val performedBy: String,

    @Column(length = 100)
    val deviceId: String? = null,

    @Column(length = 100)
    val tabletId: String? = null,

    /**
     * Additional context as JSON
     */
    @Column(columnDefinition = "TEXT")
    val auditContext: String? = null,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now(),

    @Column(length = 45)
    val ipAddress: String? = null,

    @Column(length = 500)
    val userAgent: String? = null,

    @Column(length = 500)
    val errorDetails: String? = null
)