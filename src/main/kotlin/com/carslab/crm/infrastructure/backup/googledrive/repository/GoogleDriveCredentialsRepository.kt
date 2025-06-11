// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/repository/GoogleDriveCredentialsRepository.kt
package com.carslab.crm.infrastructure.backup.googledrive.repository

import com.carslab.crm.infrastructure.backup.googledrive.entity.GoogleDriveCredentialsEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GoogleDriveCredentialsRepository : JpaRepository<GoogleDriveCredentialsEntity, Long> {
    fun findByCompanyId(companyId: Long): GoogleDriveCredentialsEntity?
    fun findByCompanyIdAndIsActive(companyId: Long, isActive: Boolean): GoogleDriveCredentialsEntity?
}