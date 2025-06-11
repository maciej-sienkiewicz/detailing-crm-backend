// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/entity/GoogleDriveCredentialsEntity.kt
package com.carslab.crm.infrastructure.backup.googledrive.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "google_drive_credentials",
    indexes = [
        Index(name = "idx_google_drive_company_id", columnList = "company_id")
    ]
)
data class GoogleDriveCredentialsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_id", nullable = false, unique = true)
    val companyId: Long,

    @Column(name = "encrypted_credentials", nullable = false, columnDefinition = "TEXT")
    val encryptedCredentials: String,

    @Column(name = "service_account_email", nullable = false)
    val serviceAccountEmail: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)