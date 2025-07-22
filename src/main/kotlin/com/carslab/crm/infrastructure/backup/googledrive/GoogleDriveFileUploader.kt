// src/main/kotlin/com/carslab/crm/infrastructure/backup/googledrive/GoogleDriveFileUploader.kt
package com.carslab.crm.infrastructure.backup.googledrive

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GoogleDriveFileUploader {
    private val logger = LoggerFactory.getLogger(GoogleDriveFileUploader::class.java)

    fun uploadFileToFolder(
        driveService: Drive,
        targetFolderId: String,
        fileData: ByteArray,
        fileName: String,
        folderPath: String,
        mimeType: String,
        metadata: Map<String, String> = emptyMap()
    ): UploadResult {

        return try {
            logger.debug("Uploading file {} to Google Drive folder: {} in path: {}", fileName, targetFolderId, folderPath)

            // Utwórz strukturę folderów w ramach udostępnionego folderu
            val finalFolderId = createOrFindFolderStructure(driveService, targetFolderId, folderPath)

            // Przygotuj metadane pliku
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(finalFolderId)

                // Dodaj custom properties jeśli są dostępne
                if (metadata.isNotEmpty()) {
                    properties = metadata
                }
            }

            // Przygotuj zawartość pliku
            val mediaContent = ByteArrayContent(mimeType, fileData)

            // Upload pliku
            val uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id,name,size,createdTime,webViewLink")
                .execute()

            logger.debug("Successfully uploaded file {} with ID: {}", fileName, uploadedFile.id)

            UploadResult(
                success = true,
                fileId = uploadedFile.id,
                fileName = uploadedFile.name,
                fileSize = uploadedFile.getSize(),
                webViewLink = uploadedFile.webViewLink
            )

        } catch (e: Exception) {
            logger.error("Failed to upload file {} to Google Drive", fileName, e)
            UploadResult(
                success = false,
                error = e.message ?: "Unknown error during upload"
            )
        }
    }

    private fun createOrFindFolderStructure(driveService: Drive, parentFolderId: String, folderPath: String): String {
        val pathParts = folderPath.split("/").filter { it.isNotBlank() }
        var currentParentId = parentFolderId

        logger.debug("Creating folder structure: {} in parent: {}", folderPath, parentFolderId)

        pathParts.forEach { folderName ->
            currentParentId = findOrCreateSubfolder(driveService, folderName, currentParentId)
        }

        return currentParentId
    }

    private fun findOrCreateSubfolder(driveService: Drive, folderName: String, parentId: String): String {
        // Sprawdź czy folder już istnieje
        val query = "name='$folderName' and '$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"

        val result = driveService.files()
            .list()
            .setQ(query)
            .setFields("files(id,name)")
            .execute()

        val existingFolder = result.files.firstOrNull()
        if (existingFolder != null) {
            logger.debug("Found existing subfolder: {} with ID: {}", folderName, existingFolder.id)
            return existingFolder.id
        }

        // Utwórz nowy folder
        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(parentId)
        }

        val createdFolder = driveService.files()
            .create(folderMetadata)
            .setFields("id,name")
            .execute()

        logger.debug("Created new subfolder: {} with ID: {}", folderName, createdFolder.id)
        return createdFolder.id
    }
}

data class UploadResult(
    val success: Boolean,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val webViewLink: String? = null,
    val error: String? = null
)