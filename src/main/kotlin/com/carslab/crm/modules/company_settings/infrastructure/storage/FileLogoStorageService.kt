package com.carslab.crm.company_settings.infrastructure.storage

import com.carslab.crm.company_settings.domain.LogoMetadata
import com.carslab.crm.company_settings.domain.LogoStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileLogoStorageService(
    @Value("\${file.logo-dir:logos}") private val logoDir: String,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String
) : LogoStorageService {

    private val rootLocation: Path = Paths.get(logoDir)

    init {
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw RuntimeException("Could not initialize logo storage location", e)
        }
    }

    override fun storeLogo(companyId: Long, logoFile: MultipartFile): LogoMetadata {
        try {
            if (logoFile.isEmpty) {
                throw RuntimeException("Failed to store empty logo file")
            }

            // Generate unique file ID
            val fileId = UUID.randomUUID().toString()

            // Create company directory if it doesn't exist
            val companyDir = rootLocation.resolve(companyId.toString())
            Files.createDirectories(companyDir)

            // Store file with generated ID as filename (keeping original extension)
            val originalFilename = logoFile.originalFilename ?: "logo"
            val extension = originalFilename.substringAfterLast('.', "")
            val targetFilename = "$fileId.$extension"
            val targetPath = companyDir.resolve(targetFilename)

            Files.copy(logoFile.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            val logoUrl = "$baseUrl/api/company-settings/logo/$fileId"

            return LogoMetadata(
                fileId = fileId,
                fileName = originalFilename,
                contentType = logoFile.contentType ?: "application/octet-stream",
                size = logoFile.size,
                url = logoUrl
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to store logo file", e)
        }
    }

    override fun deleteLogo(logoFileId: String): Boolean {
        try {
            // Find file in all company directories
            Files.walk(rootLocation, 2)
                .filter { path ->
                    path.fileName.toString().startsWith(logoFileId) &&
                            Files.isRegularFile(path)
                }
                .forEach { Files.deleteIfExists(it) }
            return true
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete logo file", e)
        }
    }

    override fun getLogoUrl(logoFileId: String): String? {
        return if (logoExists(logoFileId)) {
            "$baseUrl/api/company-settings/logo/$logoFileId"
        } else null
    }

    override fun logoExists(logoFileId: String): Boolean {
        return try {
            Files.walk(rootLocation, 2)
                .anyMatch { path ->
                    path.fileName.toString().startsWith(logoFileId) &&
                            Files.isRegularFile(path)
                }
        } catch (e: Exception) {
            false
        }
    }

    override fun getLogoPath(logoFileId: String): Path? {
        return try {
            Files.walk(rootLocation, 2)
                .filter { path ->
                    path.fileName.toString().startsWith(logoFileId) &&
                            Files.isRegularFile(path)
                }
                .findFirst()
                .orElse(null)
        } catch (e: Exception) {
            null
        }
    }
}