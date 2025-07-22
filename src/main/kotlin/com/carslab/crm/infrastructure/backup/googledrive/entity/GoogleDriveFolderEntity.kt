// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/entity/GoogleDriveFolderEntity.kt
package com.carslab.crm.infrastructure.backup.googledrive.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "google_drive_folders",
    indexes = [
        Index(name = "idx_google_drive_folder_company_id", columnList = "company_id"),
        Index(name = "idx_google_drive_folder_active", columnList = "is_active")
    ]
)
data class GoogleDriveFolderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_id", nullable = false, unique = true)
    val companyId: Long,

    @Column(name = "folder_id", nullable = false, length = 200)
    val folderId: String,

    @Column(name = "folder_name", nullable = false, length = 500)
    val folderName: String,

    @Column(name = "folder_url", length = 1000)
    val folderUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_backup_at")
    val lastBackupAt: LocalDateTime? = null,

    @Column(name = "last_backup_status", length = 50)
    val lastBackupStatus: String? = null,

    @Column(name = "backup_count", nullable = false)
    val backupCount: Long = 0,

    @Column(name = "notes", columnDefinition = "TEXT")
    val notes: String? = null
)