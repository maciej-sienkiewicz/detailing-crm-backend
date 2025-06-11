// src/main/kotlin/com/carslab/crm/infrastructure/backup/api/GoogleDriveBackupController.kt
package com.carslab.crm.infrastructure.backup.api

import com.carslab.crm.infrastructure.backup.GoogleDriveBackupService
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveCredentialsService
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/google-drive")
class GoogleDriveBackupController(
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val googleDriveCredentialsService: GoogleDriveCredentialsService
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveBackupController::class.java)

    @PostMapping("/backup-current-month")
    fun backupCurrentMonthInvoices(): ResponseEntity<Map<String, String>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return try {
            googleDriveBackupService.backupCurrentMonthInvoicesToGoogleDrive(companyId)
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Backup completed successfully"
            ))
        } catch (e: Exception) {
            logger.error("Backup failed for company {}", companyId, e)
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Backup failed")
            ))
        }
    }

    @PostMapping("/credentials")
    fun uploadCredentials(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("serviceAccountEmail") serviceAccountEmail: String
    ): ResponseEntity<Map<String, String>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return try {
            val credentialsJson = String(file.bytes)
            googleDriveCredentialsService.saveCredentials(companyId, credentialsJson, serviceAccountEmail)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Google Drive credentials saved successfully"
            ))
        } catch (e: Exception) {
            logger.error("Failed to save credentials for company {}", companyId, e)
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Failed to save credentials")
            ))
        }
    }

    @GetMapping("/integration-status")
    fun getIntegrationStatus(): ResponseEntity<Map<String, Any>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val isActive = googleDriveCredentialsService.isIntegrationActive(companyId)

        return ResponseEntity.ok(mapOf(
            "companyId" to companyId,
            "isActive" to isActive,
            "status" to if (isActive) "enabled" else "disabled"
        ))
    }
}