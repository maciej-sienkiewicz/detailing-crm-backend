package com.carslab.crm.signature.infrastructure.persistance.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "tablet_devices",
    indexes = [
        Index(name = "idx_tablet_devices_company_id", columnList = "company_id"),
        Index(name = "idx_tablet_devices_location_id", columnList = "location_id"),
        Index(name = "idx_tablet_devices_device_token", columnList = "device_token", unique = true),
        Index(name = "idx_tablet_devices_status", columnList = "status"),
        Index(name = "idx_tablet_devices_last_seen", columnList = "last_seen")
    ]
)
data class TabletDevice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "location_id", nullable = false)
    val locationId: Long,

    @Column(name = "device_token", nullable = false, unique = true, length = 255)
    val deviceToken: String,

    @Column(name = "friendly_name", nullable = false, length = 100)
    val friendlyName: String,

    @Column(name = "workstation_id")
    val workstationId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: DeviceStatus = DeviceStatus.ACTIVE,

    @Column(name = "last_seen", nullable = false)
    val lastSeen: Instant = Instant.now(),

    @Column(name = "device_info", columnDefinition = "TEXT")
    val deviceInfo: String? = null, // JSON with device details like OS, version, etc.
) : AuditableEntity()

@Entity
@Table(
    name = "workstations",
    indexes = [
        Index(name = "idx_workstations_company_id", columnList = "company_id"),
        Index(name = "idx_workstations_location_id", columnList = "location_id"),
        Index(name = "idx_workstations_paired_tablet_id", columnList = "paired_tablet_id")
    ]
)
data class Workstation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "location_id", nullable = false)
    val locationId: Long,

    @Column(name = "workstation_name", nullable = false, length = 100)
    val workstationName: String,

    @Column(name = "workstation_code", length = 50)
    val workstationCode: String? = null, // Human-readable identifier

    @Column(name = "paired_tablet_id")
    val pairedTabletId: Long? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
) : AuditableEntity()



@Entity
@Table(
    name = "signature_sessions",
    indexes = [
        Index(name = "idx_signature_sessions_session_id", columnList = "session_id", unique = true),
        Index(name = "idx_signature_sessions_company_id", columnList = "company_id"),
        Index(name = "idx_signature_sessions_workstation_id", columnList = "workstation_id"),
        Index(name = "idx_signature_sessions_assigned_tablet_id", columnList = "assigned_tablet_id"),
        Index(name = "idx_signature_sessions_status", columnList = "status"),
        Index(name = "idx_signature_sessions_created_at", columnList = "created_at"),
        Index(name = "idx_signature_sessions_expires_at", columnList = "expires_at"),
        Index(name = "idx_signature_sessions_customer_name", columnList = "customer_name")
    ]
)
data class SignatureSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "session_id", nullable = false, unique = true, length = 50)
    val sessionId: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "workstation_id", nullable = false)
    val workstationId: Long,

    @Column(name = "assigned_tablet_id")
    val assignedTabletId: Long? = null,

    @Column(name = "customer_name", nullable = false, length = 255)
    val customerName: String,

    @Column(name = "customer_email", length = 255)
    val customerEmail: String? = null,

    @Column(name = "customer_phone", length = 50)
    val customerPhone: String? = null,

    // Vehicle information as separate columns for better querying
    @Column(name = "vehicle_make", length = 100)
    val vehicleMake: String? = null,

    @Column(name = "vehicle_model", length = 100)
    val vehicleModel: String? = null,

    @Column(name = "vehicle_license_plate", length = 20)
    val vehicleLicensePlate: String? = null,

    @Column(name = "vehicle_vin", length = 17)
    val vehicleVin: String? = null,

    @Column(name = "vehicle_year")
    val vehicleYear: Int? = null,

    @Column(name = "vehicle_color", length = 50)
    val vehicleColor: String? = null,

    @Column(name = "service_type", length = 100)
    val serviceType: String? = null,

    @Column(name = "document_type", length = 100)
    val documentType: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SignatureStatus,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Lob
    @Column(name = "signature_image", columnDefinition = "LONGTEXT")
    var signatureImage: String? = null,

    @Column(name = "signed_at")
    var signedAt: Instant? = null,

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    val additionalNotes: String? = null,

    // Metadata for audit and analytics
    @Column(name = "client_ip", length = 45)
    val clientIp: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "signature_duration_seconds")
    var signatureDurationSeconds: Int? = null

) : AuditableEntity()

enum class SignatureStatus {
    PENDING,        // Created, waiting for tablet assignment
    SENT_TO_TABLET, // Sent to tablet, waiting for signature
    COMPLETED,      // Signature completed successfully
    EXPIRED,        // Session expired without signature
    CANCELLED,      // Cancelled by user
    FAILED          // Technical failure during processing
}

enum class DataRetentionPolicy(val retentionDays: Long) {
    BUSINESS_REQUIREMENT(2555), // 7 years for legal compliance
    GDPR_MINIMAL(30),          // 30 days minimal retention
    AUDIT_EXTENDED(1095),      // 3 years for audit purposes
    CUSTOM(365)                // 1 year default
}

@Entity
@Table(
    name = "pairing_codes",
    indexes = [
        Index(name = "idx_pairing_codes_company_id", columnList = "company_id"),
        Index(name = "idx_pairing_codes_expires_at", columnList = "expires_at")
    ]
)

data class PairingCode(
    @Id
    @Column(name = "code", length = 10)
    val code: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "location_id", nullable = false)
    val locationId: Long,

    @Column(name = "workstation_id")
    val workstationId: Long? = null,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "device_name", length = 100)
    val deviceName: String? = null, // Expected device name

    @Column(name = "created_by_user_id")
    val createdByUserId: Long? = null,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "used_by_tablet_id")
    var usedByTabletId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)


enum class DeviceStatus {
    ACTIVE,      // Device is active and can receive requests
    INACTIVE,    // Device is inactive (manually disabled)
    MAINTENANCE, // Device is in maintenance mode
    ERROR        // Device has errors and needs attention
}

enum class SignatureSessionStatus {
    PENDING, SENT_TO_TABLET, SIGNED, EXPIRED, CANCELLED
}