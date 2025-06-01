package com.carslab.crm.signature.infrastructure.persistance.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
data class TabletDevice(
    @Id val id: UUID,
    val tenantId: UUID,
    val locationId: UUID,
    val deviceToken: String, // unikalny token urządzenia
    val friendlyName: String, // np. "Tablet Recepcja 1"
    val workstationId: UUID?, // opcjonalne powiązanie ze stanowiskiem
    val status: DeviceStatus,
    val lastSeen: Instant
)

@Entity
data class Workstation(
    @Id val id: UUID,
    val tenantId: UUID,
    val locationId: UUID,
    val workstationName: String,
    val pairedTabletId: UUID? // powiązany tablet
)