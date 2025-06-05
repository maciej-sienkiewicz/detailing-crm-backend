package com.carslab.crm.signature.infrastructure.persistance.entity

import com.carslab.crm.signature.api.dto.SimpleSignatureStatus
import com.carslab.crm.signature.api.dto.SimpleSignatureType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "simple_signature_sessions",
    indexes = [
        Index(name = "idx_simple_sig_sessions_company_id", columnList = "companyId"),
        Index(name = "idx_simple_sig_sessions_status", columnList = "status"),
        Index(name = "idx_simple_sig_sessions_expires_at", columnList = "expiresAt"),
        Index(name = "idx_simple_sig_sessions_tablet_id", columnList = "tabletId"),
        Index(name = "idx_simple_sig_sessions_created_at", columnList = "createdAt"),
        Index(name = "idx_simple_sig_sessions_signer_name", columnList = "signerName"),
        Index(name = "idx_simple_sig_sessions_external_ref", columnList = "externalReference")
    ]
)
data class SimpleSignatureSession(
    @Id
    val sessionId: UUID = UUID.randomUUID(),

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
    @Column(nullable = false, length = 20)
    val signatureType: SimpleSignatureType = SimpleSignatureType.GENERAL,

    /**
     * External reference to link with other entities
     */
    @Column(length = 100)
    val externalReference: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: SimpleSignatureStatus = SimpleSignatureStatus.PENDING,

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
    fun updateStatus(newStatus: SimpleSignatureStatus, errorMessage: String? = null): SimpleSignatureSession {
        return this.copy(
            status = newStatus,
            updatedAt = Instant.now(),
            errorMessage = errorMessage
        )
    }

    fun markAsSigned(signatureImagePath: String): SimpleSignatureSession {
        return this.copy(
            status = SimpleSignatureStatus.COMPLETED,
            signedAt = Instant.now(),
            signatureImagePath = signatureImagePath,
            updatedAt = Instant.now()
        )
    }

    fun cancel(cancelledBy: String, reason: String? = null): SimpleSignatureSession {
        return this.copy(
            status = SimpleSignatureStatus.CANCELLED,
            cancelledBy = cancelledBy,
            cancelledAt = Instant.now(),
            cancellationReason = reason,
            updatedAt = Instant.now()
        )
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun canBeSigned(): Boolean = status == SimpleSignatureStatus.SENT_TO_TABLET && !isExpired()
}