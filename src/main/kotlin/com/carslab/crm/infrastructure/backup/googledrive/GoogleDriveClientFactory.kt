// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/GoogleDriveClientFactory.kt
package com.carslab.crm.infrastructure.backup.googledrive
// Dodaj ten import zamiast powyższego:
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class GoogleDriveClientFactory(
    private val googleDriveCredentialsService: GoogleDriveCredentialsService
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveClientFactory::class.java)
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    fun createDriveService(companyId: Long): Drive {
        logger.debug("Creating Google Drive service for company: {}", companyId)

        try {
            // Pobierz credentials dla danego company
            val credentialsJson = googleDriveCredentialsService.getCredentials(companyId)

            // Utwórz credentials z JSON (nowsze API)
            val credentials = ServiceAccountCredentials.fromStream(
                ByteArrayInputStream(credentialsJson.toByteArray())
            ).createScoped(listOf(DriveScopes.DRIVE_FILE))

            // Utwórz Drive service
            return Drive.Builder(
                httpTransport,
                jsonFactory,
                HttpCredentialsAdapter(credentials)
            )
                .setApplicationName("CRM-GoogleDrive-Backup")
                .build()

        } catch (e: Exception) {
            logger.error("Failed to create Google Drive service for company {}", companyId, e)
            throw RuntimeException("Failed to initialize Google Drive client for company $companyId", e)
        }
    }
}