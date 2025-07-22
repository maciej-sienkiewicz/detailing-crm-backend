// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/repository/GoogleDriveFolderRepository.kt
package com.carslab.crm.infrastructure.backup.googledrive.repository

import com.carslab.crm.infrastructure.backup.googledrive.entity.GoogleDriveFolderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface GoogleDriveFolderRepository : JpaRepository<GoogleDriveFolderEntity, Long> {

    fun findByCompanyId(companyId: Long): GoogleDriveFolderEntity?

    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): GoogleDriveFolderEntity?

    fun existsByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): Boolean

    fun findByFolderId(folderId: String): GoogleDriveFolderEntity?

    fun findAllByIsActive(isActive: Boolean): List<GoogleDriveFolderEntity>

    @Modifying
    @Query("UPDATE GoogleDriveFolderEntity g SET g.lastBackupAt = :backupTime, g.lastBackupStatus = :status, g.backupCount = g.backupCount + 1 WHERE g.companyId = :companyId")
    fun updateBackupInfo(
        @Param("companyId") companyId: Long,
        @Param("backupTime") backupTime: LocalDateTime,
        @Param("status") status: String
    ): Int

    @Query("SELECT COUNT(g) FROM GoogleDriveFolderEntity g WHERE g.isActive = true")
    fun countActiveIntegrations(): Long
}