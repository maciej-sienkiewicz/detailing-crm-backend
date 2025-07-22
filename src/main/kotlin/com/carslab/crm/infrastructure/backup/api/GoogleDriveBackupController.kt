// src/main/kotlin/com/carslab/crm/infrastructure/backup/api/GoogleDriveBackupController.kt
package com.carslab.crm.infrastructure.backup.api

import com.carslab.crm.infrastructure.backup.GoogleDriveBackupService
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveSystemService
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveFolderService
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/google-drive")
@Tag(name = "Google Drive Backup", description = "Google Drive backup integration endpoints")
class GoogleDriveBackupController(
    private val googleDriveBackupService: GoogleDriveBackupService,
    private val googleDriveFolderService: GoogleDriveFolderService,
    private val googleDriveSystemService: GoogleDriveSystemService
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveBackupController::class.java)

    @PostMapping("/backup-current-month")
    @Operation(summary = "Backup current month invoices", description = "Backs up all invoices from current month to configured Google Drive folder")
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

    @PostMapping("/configure-folder")
    @Operation(summary = "Configure Google Drive folder", description = "Configure Google Drive folder for backup by providing folder ID")
    fun configureFolder(
        @RequestBody request: ConfigureFolderRequest
    ): ResponseEntity<Map<String, Any>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return try {
            val folderEntity = googleDriveFolderService.configureFolderForCompany(
                companyId = companyId,
                folderId = request.folderId,
                folderName = request.folderName
            )

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Google Drive folder configured successfully",
                "data" to mapOf(
                    "folder_id" to folderEntity.folderId,
                    "folder_name" to folderEntity.folderName,
                    "folder_url" to folderEntity.folderUrl,
                    "system_email" to googleDriveSystemService.getSystemEmail()
                )
            ))
        } catch (e: Exception) {
            logger.error("Failed to configure folder for company {}", companyId, e)
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Failed to configure folder")
            ))
        }
    }

    @GetMapping("/integration-status")
    @Operation(summary = "Get integration status", description = "Get current Google Drive integration status for the company")
    fun getIntegrationStatus(): ResponseEntity<Map<String, Any>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val folderConfig = googleDriveFolderService.getFolderConfigurationForCompany(companyId)
        val isActive = folderConfig?.isActive == true

        val responseMap = mutableMapOf<String, Any>(
            "company_id" to companyId,
            "is_active" to isActive,
            "status" to if (isActive) "enabled" else "disabled",
            "system_email" to googleDriveSystemService.getSystemEmail(),
            "system_service_available" to googleDriveSystemService.isSystemServiceAvailable()
        )

        // Dodaj konfigurację tylko jeśli istnieje
        if (folderConfig != null) {
            responseMap["configuration"] = mapOf(
                "folder_id" to folderConfig.folderId,
                "folder_name" to folderConfig.folderName,
                "folder_url" to (folderConfig.folderUrl ?: ""),
                "last_backup_at" to (folderConfig.lastBackupAt?.toString() ?: ""),
                "last_backup_status" to (folderConfig.lastBackupStatus ?: ""),
                "backup_count" to folderConfig.backupCount
            )
        }

        return ResponseEntity.ok(responseMap)
    }

    @PostMapping("/validate-folder")
    @Operation(summary = "Validate folder access", description = "Validate if the system can access the provided Google Drive folder")
    fun validateFolder(
        @RequestBody request: ValidateFolderRequest
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val isValid = googleDriveFolderService.validateFolderAccess(request.folderId)

            val responseMap = mutableMapOf<String, Any>(
                "status" to "success",
                "valid" to isValid,
                "message" to if (isValid) "Folder is accessible" else "Cannot access folder",
                "systemEmail" to googleDriveSystemService.getSystemEmail()
            )

            // Dodaj instrukcje tylko jeśli folder nie jest dostępny
            if (!isValid) {
                responseMap["instructions"] = mapOf(
                    "step1" to "Make sure the folder exists and is shared",
                    "step2" to "Share the folder with: ${googleDriveSystemService.getSystemEmail()}",
                    "step3" to "Grant 'Editor' permissions to the system account"
                )
            }

            ResponseEntity.ok(responseMap)
        } catch (e: Exception) {
            logger.error("Failed to validate folder: {}", request.folderId, e)
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "valid" to false,
                "message" to (e.message ?: "Folder validation failed"),
                "systemEmail" to googleDriveSystemService.getSystemEmail()
            ))
        }
    }

    @DeleteMapping("/integration")
    @Operation(summary = "Deactivate integration", description = "Deactivate Google Drive integration for the company")
    fun deactivateIntegration(): ResponseEntity<Map<String, String>> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return try {
            val deactivated = googleDriveFolderService.deactivateIntegrationForCompany(companyId)

            if (deactivated) {
                ResponseEntity.ok(mapOf(
                    "status" to "success",
                    "message" to "Google Drive integration deactivated successfully"
                ))
            } else {
                ResponseEntity.badRequest().body(mapOf(
                    "status" to "error",
                    "message" to "No active integration found"
                ))
            }
        } catch (e: Exception) {
            logger.error("Failed to deactivate integration for company {}", companyId, e)
            ResponseEntity.badRequest().body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Failed to deactivate integration")
            ))
        }
    }

    @GetMapping("/system-info")
    @Operation(summary = "Get system information", description = "Get Google Drive system service information")
    fun getSystemInfo(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "systemEmail" to googleDriveSystemService.getSystemEmail(),
            "systemServiceAvailable" to googleDriveSystemService.isSystemServiceAvailable(),
            "connectionTest" to try {
                googleDriveSystemService.testConnection()
            } catch (e: Exception) {
                false
            },
            "stats" to googleDriveFolderService.getIntegrationStats(),
            "instructions" to mapOf(
                "step1" to "Create a folder in your Google Drive",
                "step2" to "Share the folder with: ${googleDriveSystemService.getSystemEmail()}",
                "step3" to "Grant 'Editor' permissions",
                "step4" to "Copy the folder ID from the URL",
                "step5" to "Use the 'Configure Folder' endpoint with the folder ID"
            )
        ))
    }
}

data class ConfigureFolderRequest(
    @field:NotBlank(message = "Folder ID is required")
    @Parameter(description = "Google Drive folder ID", required = true)
    @JsonProperty("folder_id")
    val folderId: String,

    @Parameter(description = "Optional folder name for display purposes")
    @JsonProperty("folder_name")
    val folderName: String? = null
)

data class ValidateFolderRequest(
    @field:NotBlank(message = "Folder ID is required")
    @Parameter(description = "Google Drive folder ID to validate", required = true)
    @JsonProperty("folder_id")
    val folderId: String
)