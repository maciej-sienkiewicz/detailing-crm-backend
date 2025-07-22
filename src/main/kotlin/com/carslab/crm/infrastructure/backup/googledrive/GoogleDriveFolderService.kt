// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/GoogleDriveFolderService.kt
package com.carslab.crm.infrastructure.backup.googledrive

import com.carslab.crm.infrastructure.backup.googledrive.entity.GoogleDriveFolderEntity
import com.carslab.crm.infrastructure.backup.googledrive.repository.GoogleDriveFolderRepository
import com.google.api.services.drive.model.File
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class GoogleDriveFolderService(
    private val googleDriveFolderRepository: GoogleDriveFolderRepository,
    private val googleDriveSystemService: GoogleDriveSystemService
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveFolderService::class.java)

    fun configureFolderForCompany(
        companyId: Long,
        folderId: String,
        folderName: String? = null
    ): GoogleDriveFolderEntity {
        logger.info("Configuring Google Drive folder for company: {} with folder ID: {}", companyId, folderId)

        try {
            // Sprawdź czy folder istnieje i czy mamy do niego dostęp
            val folderInfo = validateAndGetFolderInfo(folderId)

            // Sprawdź czy folder nie jest już używany przez inną firmę
            val existingFolder = googleDriveFolderRepository.findByFolderId(folderId)
            if (existingFolder != null && existingFolder.companyId != companyId) {
                throw RuntimeException("Folder ID $folderId is already used by another company")
            }

            // Usuń poprzednią konfigurację dla tej firmy jeśli istnieje
            googleDriveFolderRepository.findByCompanyId(companyId)?.let { existing ->
                googleDriveFolderRepository.delete(existing)
                logger.info("Removed previous folder configuration for company: {}", companyId)
            }

            // Utwórz nową konfigurację
            val folderEntity = GoogleDriveFolderEntity(
                companyId = companyId,
                folderId = folderId,
                folderName = folderName ?: folderInfo.name,
                folderUrl = "https://drive.google.com/drive/folders/$folderId",
                isActive = true,
                notes = "Configured for system account: ${googleDriveSystemService.getSystemEmail()}"
            )

            val savedEntity = googleDriveFolderRepository.save(folderEntity)
            logger.info("Successfully configured Google Drive folder for company: {}", companyId)

            return savedEntity

        } catch (e: Exception) {
            logger.error("Failed to configure Google Drive folder for company: {}", companyId, e)
            throw RuntimeException("Failed to configure Google Drive folder: ${e.message}", e)
        }
    }

    @Transactional(readOnly = true)
    fun getFolderConfigurationForCompany(companyId: Long): GoogleDriveFolderEntity? {
        return googleDriveFolderRepository.findByCompanyIdAndIsActive(companyId, true)
    }

    @Transactional(readOnly = true)
    fun isIntegrationActiveForCompany(companyId: Long): Boolean {
        return googleDriveFolderRepository.existsByCompanyIdAndIsActive(companyId, true)
    }

    fun deactivateIntegrationForCompany(companyId: Long): Boolean {
        logger.info("Deactivating Google Drive integration for company: {}", companyId)

        val folderEntity = googleDriveFolderRepository.findByCompanyId(companyId)
        return if (folderEntity != null) {
            val deactivatedEntity = folderEntity.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )
            googleDriveFolderRepository.save(deactivatedEntity)
            logger.info("Successfully deactivated Google Drive integration for company: {}", companyId)
            true
        } else {
            logger.warn("No Google Drive configuration found for company: {}", companyId)
            false
        }
    }

    fun updateBackupStatus(companyId: Long, status: String, backupTime: LocalDateTime = LocalDateTime.now()) {
        googleDriveFolderRepository.updateBackupInfo(companyId, backupTime, status)
        logger.debug("Updated backup status for company {}: {}", companyId, status)
    }

    @Transactional(readOnly = true)
    fun getAllActiveIntegrations(): List<GoogleDriveFolderEntity> {
        return googleDriveFolderRepository.findAllByIsActive(true)
    }

    @Transactional(readOnly = true)
    fun getIntegrationStats(): Map<String, Any> {
        val activeCount = googleDriveFolderRepository.countActiveIntegrations()
        val totalCount = googleDriveFolderRepository.count()

        return mapOf(
            "activeIntegrations" to activeCount,
            "totalIntegrations" to totalCount,
            "systemEmail" to googleDriveSystemService.getSystemEmail(),
            "systemServiceAvailable" to googleDriveSystemService.isSystemServiceAvailable()
        )
    }

    fun validateFolderAccess(folderId: String): Boolean {
        return try {
            validateAndGetFolderInfo(folderId)
            true
        } catch (e: Exception) {
            logger.warn("Folder validation failed for ID: {}", folderId, e)
            false
        }
    }

    private fun validateAndGetFolderInfo(folderId: String): File {
        if (!googleDriveSystemService.isSystemServiceAvailable()) {
            throw IllegalStateException("Google Drive system service is not available")
        }

        val driveService = googleDriveSystemService.getSystemDriveService()

        try {
            val folder = driveService.files()
                .get(folderId)
                .setFields("id,name,mimeType,capabilities")
                .execute()

            // Sprawdź czy to rzeczywiście folder
            if (folder.mimeType != "application/vnd.google-apps.folder") {
                throw RuntimeException("Provided ID is not a folder")
            }

            // Sprawdź czy mamy uprawnienia do zapisu
            val capabilities = folder.capabilities
            if (capabilities == null || !capabilities.canAddChildren) {
                throw RuntimeException("No write permissions to folder. Please share folder with: ${googleDriveSystemService.getSystemEmail()}")
            }

            logger.debug("Folder validation successful: {} ({})", folder.name, folderId)
            return folder

        } catch (e: Exception) {
            logger.error("Failed to validate folder access for ID: {}", folderId, e)
            throw RuntimeException("Cannot access folder: ${e.message}")
        }
    }
}