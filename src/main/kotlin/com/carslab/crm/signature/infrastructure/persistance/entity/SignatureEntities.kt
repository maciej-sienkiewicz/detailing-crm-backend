// src/main/kotlin/com/carslab/crm/signature/infrastructure/persistance/entity/SignatureEntities.kt
package com.carslab.crm.signature.infrastructure.persistance.entity

import com.carslab.crm.signature.api.dto.SignatureStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "signature_sessions",
    indexes = [
        Index(name = "idx_signature_sessions_company_id", columnList = "companyId"),
        Index(name = "idx_signature_sessions_status", columnList = "status"),
        Index(name = "idx_signature_sessions_expires_at", columnList = "expiresAt"),
        Index(name = "idx_signature_sessions_workstation_id", columnList = "workstationId"),
        Index(name = "idx_signature_sessions_tablet_id", columnList = "tabletId"),
        Index(name = "idx_signature_sessions_created_at", columnList = "createdAt")
    ]
)
data class SignatureSession(
    @Id
    val sessionId: UUID = UUID.randomUUID(),

    val workstationId: UUID? = null,

    @Column(nullable = false)
    val tabletId: UUID,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 200)
    val customerName: String,

    /**
     * Vehicle information as JSON
     */
    @Column(columnDefinition = "TEXT")
    val vehicleInfo: String? = null,

    @Column(length = 100)
    val serviceType: String? = null,

    @Column(length = 100)
    val documentType: String? = null,

    @Column(columnDefinition = "TEXT")
    val additionalNotes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: SignatureStatus = SignatureStatus.PENDING,

    @Column(nullable = false, length = 100)
    val createdBy: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val expiresAt: Instant,

    val signedAt: Instant? = null,

    @Column(length = 500)
    val signatureImagePath: String? = null,

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
    fun updateStatus(newStatus: SignatureStatus, errorMessage: String? = null): SignatureSession {
        return this.copy(
            status = newStatus,
            updatedAt = Instant.now(),
            errorMessage = errorMessage
        )
    }

    fun markAsSigned(signatureImagePath: String): SignatureSession {
        return this.copy(
            status = SignatureStatus.COMPLETED,
            signedAt = Instant.now(),
            signatureImagePath = signatureImagePath,
            updatedAt = Instant.now()
        )
    }

    fun cancel(cancelledBy: String, reason: String? = null): SignatureSession {
        return this.copy(
            status = SignatureStatus.CANCELLED,
            cancelledBy = cancelledBy,
            cancelledAt = Instant.now(),
            cancellationReason = reason,
            updatedAt = Instant.now()
        )
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun canBeSigned(): Boolean = status == SignatureStatus.SENT_TO_TABLET && !isExpired()
}

/**
 * Audit log for signature operations
 */
@Entity
@Table(
    name = "signature_audit_log",
    indexes = [
        Index(name = "idx_signature_audit_session_id", columnList = "sessionId"),
        Index(name = "idx_signature_audit_company_id", columnList = "companyId"),
        Index(name = "idx_signature_audit_timestamp", columnList = "timestamp"),
        Index(name = "idx_signature_audit_action", columnList = "action")
    ]
)
data class SignatureAuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val sessionId: UUID,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false, length = 50)
    val action: String, // CREATED, SENT_TO_TABLET, SIGNED, CANCELLED, DOWNLOADED, etc.

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