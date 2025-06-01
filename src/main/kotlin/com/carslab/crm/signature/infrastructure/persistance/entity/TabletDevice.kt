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
        Index(name = "idx_signature_sessions_session_id", columnList = "session_id", unique = true),
        Index(name = "idx_signature_sessions_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_signature_sessions_workstation_id", columnList = "workstation_id"),
        Index(name = "idx_signature_sessions_status", columnList = "status"),
        Index(name = "idx_signature_sessions_created_at", columnList = "created_at"),
        Index(name = "idx_signature_sessions_expires_at", columnList = "expires_at")
    ]
)
data class SignatureSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    val sessionId: String,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "workstation_id", nullable = false)
    val workstationId: UUID,

    @Column(name = "customer_name", nullable = false, length = 255)
    val customerName: String,

    @Column(name = "vehicle_info", length = 500)
    val vehicleInfo: String?,

    @Column(name = "service_type", length = 100)
    val serviceType: String?,

    @Column(name = "document_type", length = 100)
    val documentType: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SignatureStatus,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Lob
    @Column(name = "signature_image", columnDefinition = "TEXT")
    var signatureImage: String?,

    @Column(name = "signed_at")
    var signedAt: Instant?
) : AuditableEntity()

enum class SignatureStatus {
    PENDING,    // Oczekuje na podpis
    COMPLETED,  // Podpisane pomyślnie
    EXPIRED,    // Sesja wygasła
    CANCELLED,  // Anulowane przez użytkownika
    FAILED      // Błąd podczas przetwarzania
}

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