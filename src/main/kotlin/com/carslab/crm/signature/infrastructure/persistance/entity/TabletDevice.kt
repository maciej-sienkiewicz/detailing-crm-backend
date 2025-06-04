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