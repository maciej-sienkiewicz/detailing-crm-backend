// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/GoogleDriveCredentialsService.kt
package com.carslab.crm.infrastructure.backup.googledrive

import com.carslab.crm.infrastructure.backup.googledrive.entity.GoogleDriveCredentialsEntity
import com.carslab.crm.infrastructure.backup.googledrive.repository.GoogleDriveCredentialsRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Service

@Service
class GoogleDriveCredentialsService(
    private val credentialsRepository: GoogleDriveCredentialsRepository,
    private val textEncryptor: TextEncryptor
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveCredentialsService::class.java)

    fun getCredentials(companyId: Long): String {
        logger.debug("Retrieving Google Drive credentials for company: {}", companyId)

        val entity = credentialsRepository.findByCompanyId(companyId)
            ?: throw RuntimeException("Google Drive credentials not found for company $companyId")

        if (!entity.isActive) {
            throw RuntimeException("Google Drive integration is disabled for company $companyId")
        }

        // Odszyfruj credentials
        return try {
            textEncryptor.decrypt(entity.encryptedCredentials)
        } catch (e: Exception) {
            logger.error("Failed to decrypt Google Drive credentials for company {}", companyId, e)
            throw RuntimeException("Failed to decrypt Google Drive credentials", e)
        }
    }

    fun saveCredentials(companyId: Long, credentialsJson: String, serviceAccountEmail: String) {
        logger.info("Saving Google Drive credentials for company: {}", companyId)

        try {
            // Zaszyfruj credentials
            val encryptedCredentials = textEncryptor.encrypt(credentialsJson)

            val entity = credentialsRepository.findByCompanyId(companyId)?.let {
                it.copy(
                    encryptedCredentials = encryptedCredentials,
                    serviceAccountEmail = serviceAccountEmail,
                    isActive = true,
                    updatedAt = java.time.LocalDateTime.now()
                )
            } ?: GoogleDriveCredentialsEntity(
                companyId = companyId,
                encryptedCredentials = encryptedCredentials,
                serviceAccountEmail = serviceAccountEmail,
                isActive = true
            )

            credentialsRepository.save(entity)
            logger.info("Google Drive credentials saved successfully for company {}", companyId)

        } catch (e: Exception) {
            logger.error("Failed to save Google Drive credentials for company {}", companyId, e)
            throw RuntimeException("Failed to save Google Drive credentials", e)
        }
    }

    fun disableIntegration(companyId: Long) {
        logger.info("Disabling Google Drive integration for company: {}", companyId)

        credentialsRepository.findByCompanyId(companyId)?.let { entity ->
            credentialsRepository.save(entity.copy(
                isActive = false,
                updatedAt = java.time.LocalDateTime.now()
            ))
        }
    }

    fun isIntegrationActive(companyId: Long): Boolean {
        return credentialsRepository.findByCompanyId(companyId)?.isActive ?: false
    }
}