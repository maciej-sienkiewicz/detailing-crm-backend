package com.carslab.crm.signature.infrastructure.persistance.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tablet_devices")
data class TabletDevice(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val locationId: UUID,

    @Column(nullable = false, unique = true)
    val deviceToken: String,

    @Column(nullable = false)
    val friendlyName: String,

    val workstationId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: DeviceStatus = DeviceStatus.ACTIVE,

    @Column(nullable = false)
    val lastSeen: Instant = Instant.now(),

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "workstations")
data class Workstation(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val locationId: UUID,

    @Column(nullable = false)
    val workstationName: String,

    val pairedTabletId: UUID? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now()
)

@Entity
@Table(
    name = "signature_sessions",
    indexes = [
        Index(name = "idx_signature_sessions_tenant_status", columnList = "tenant_id, status"),
        Index(name = "idx_signature_sessions_workstation", columnList = "workstation_id"),
        Index(name = "idx_signature_sessions_expires_at", columnList = "expires_at"),
        Index(name = "idx_signature_sessions_session_id", columnList = "session_id", unique = true),
        Index(name = "idx_signature_sessions_created_at", columnList = "created_at")
    ]
)
data class SignatureSession(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 36)
    val sessionId: String,

    @Column(nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val workstationId: UUID,

    val tabletId: UUID? = null,

    @Column(nullable = false)
    val customerId: UUID,

    @Column(nullable = false, length = 100)
    val customerName: String,

    @Column(nullable = false, length = 50)
    val vehicleMake: String,

    @Column(nullable = false, length = 50)
    val vehicleModel: String,

    @Column(nullable = false, length = 20)
    val licensePlate: String,

    @Column(length = 17)
    val vin: String? = null,

    @Column(nullable = false, length = 100)
    val serviceType: String,

    @Column(nullable = false)
    val documentId: UUID,

    @Column(nullable = false, length = 50)
    val documentType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: SignatureSessionStatus = SignatureSessionStatus.PENDING,

    @Lob
    @Column(columnDefinition = "TEXT")
    val signatureImage: String? = null,

    val signedAt: Instant? = null,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val dataRetentionPolicy: DataRetentionPolicy = DataRetentionPolicy.BUSINESS_REQUIREMENT,

    @Column(nullable = false)
    val gdprConsent: Boolean = false,

    @Column(nullable = false, length = 200)
    val dataProcessingPurpose: String = "Digital signature collection"

) : AuditableEntity()

enum class DataRetentionPolicy(val retentionDays: Long) {
    BUSINESS_REQUIREMENT(2555), // 7 years
    GDPR_MINIMAL(30),
    CUSTOM(365) // 1 year default
}

@Entity
@Table(name = "pairing_codes")
data class PairingCode(
    @Id
    val code: String,

    @Column(nullable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    val locationId: UUID,

    val workstationId: UUID? = null,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class DeviceStatus {
    ACTIVE, INACTIVE, MAINTENANCE, ERROR
}

enum class SignatureSessionStatus {
    PENDING, SENT_TO_TABLET, SIGNED, EXPIRED, CANCELLED
}