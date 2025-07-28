// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/GoogleDriveSystemService.kt
package com.carslab.crm.infrastructure.backup.googledrive

import com.carslab.crm.infrastructure.backup.googledrive.config.GoogleDriveConfig
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

@Service
class GoogleDriveSystemService(
    private val googleDriveConfig: GoogleDriveConfig
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveSystemService::class.java)
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private lateinit var systemDriveService: Drive

    @PostConstruct
    fun initializeSystemDriveService() {
        logger.info("Initializing Google Drive OAuth service")

//        try {
//            if (googleDriveConfig.oauth.clientId.isBlank() ||
//                googleDriveConfig.oauth.clientSecret.isBlank() ||
//                googleDriveConfig.oauth.refreshToken.isBlank()) {
//                logger.warn("Google Drive OAuth credentials not configured. Service will be unavailable.")
//                return
//            }
//
//            // Utwórz credentials z refresh token
//            val credential = GoogleCredential.Builder()
//                .setTransport(httpTransport)
//                .setJsonFactory(jsonFactory)
//                .setClientSecrets(
//                    googleDriveConfig.oauth.clientId,
//                    googleDriveConfig.oauth.clientSecret
//                )
//                .build()
//                .setRefreshToken(googleDriveConfig.oauth.refreshToken)
//
//            // Odśwież access token
//            credential.refreshToken()
//
//            systemDriveService = Drive.Builder(
//                httpTransport,
//                jsonFactory,
//                credential
//            )
//                .setApplicationName("CRM-GoogleDrive-OAuth-Backup")
//                .build()
//
//            logger.info("Google Drive OAuth service initialized successfully with account: {}",
//                googleDriveConfig.system.email)
//
//        } catch (e: Exception) {
//            logger.error("Failed to initialize Google Drive OAuth service", e)
//            throw RuntimeException("Failed to initialize Google Drive OAuth service", e)
//        }
    }

    fun getSystemDriveService(): Drive {
        if (!::systemDriveService.isInitialized) {
            throw IllegalStateException("Google Drive OAuth service is not initialized. Check OAuth configuration.")
        }
        return systemDriveService
    }

    fun isSystemServiceAvailable(): Boolean {
        return ::systemDriveService.isInitialized && googleDriveConfig.backup.enabled
    }

    fun getSystemEmail(): String {
        return googleDriveConfig.system.email
    }

    fun testConnection(): Boolean {
        return try {
            if (!isSystemServiceAvailable()) {
                logger.warn("OAuth service is not available")
                return false
            }

            val about = systemDriveService.about()
                .get()
                .setFields("user")
                .execute()

            logger.debug("Connection test successful. Connected as: {}", about.user?.emailAddress)
            true
        } catch (e: Exception) {
            logger.error("Google Drive connection test failed", e)
            false
        }
    }
}