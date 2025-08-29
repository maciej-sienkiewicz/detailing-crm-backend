package com.carslab.crm.infrastructure.backup.googledrive.config

import com.carslab.crm.production.modules.companysettings.domain.model.GoogleDriveSettings
import org.springframework.stereotype.Service

@Service
class GoogleDriveConfigurationService(
    private val googleDriveConfig: GoogleDriveConfig
) {

    /**
     * Inicjalizuje dane Google Drive dla firmy z globalnej konfiguracji
     */
    fun initializeGoogleDriveCredentials(): GoogleDriveSettings {
        return GoogleDriveSettings(
            clientId = googleDriveConfig.oauth.clientId,
            clientSecret = googleDriveConfig.oauth.clientSecret,
            refreshToken = googleDriveConfig.oauth.refreshToken,
            defaultFolderId = null, // będzie ustawione przez użytkownika
            defaultFolderName = null, // będzie ustawione przez użytkownika
            enabled = false,
            autoUploadInvoices = false,
            autoCreateFolders = false
        )
    }

    /**
     * Sprawdza czy konfiguracja Google Drive jest poprawna
     */
    fun isConfigurationValid(): Boolean {
        return googleDriveConfig.oauth.clientId.isNotBlank() &&
                googleDriveConfig.oauth.clientSecret.isNotBlank() &&
                googleDriveConfig.oauth.refreshToken.isNotBlank()
    }

    /**
     * Zwraca email konta systemowego
     */
    fun getSystemEmail(): String {
        return googleDriveConfig.system.email
    }

    /**
     * Zwraca ustawienia backup
     */
    fun getBackupSettings(): GoogleDriveConfig.BackupSettings {
        return googleDriveConfig.backup
    }
}