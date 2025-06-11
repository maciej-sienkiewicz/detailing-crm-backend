// src/main/kotlin/com/carslab/crm/infrastructure/storage/provider/LocalFileStorageProvider.kt
package com.carslab.crm.infrastructure.storage.provider

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Component
class LocalFileStorageProvider(
    @Value("\${file.upload-dir:files}") private val baseDir: String
) : StorageProvider {

    private val logger = LoggerFactory.getLogger(LocalFileStorageProvider::class.java)
    private val rootPath: Path = Paths.get(baseDir)

    init {
        Files.createDirectories(rootPath)
    }

    override fun store(request: StoreFileRequest): StoreFileResponse {
        try {
            val targetPath = rootPath.resolve(request.fileName)
            Files.createDirectories(targetPath.parent)

            val inputStream = request.file?.inputStream ?: request.inputStream
            ?: throw IllegalArgumentException("No file or input stream provided")

            val size = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // Generuj ETag (MD5 hash)
            val etag = generateETag(targetPath)

            // Zapisz metadane jako extended attributes (Linux/macOS) lub osobny plik
            saveMetadata(targetPath, request.metadata + request.tags)

            logger.debug("Stored file: {} ({} bytes)", request.fileName, size)

            return StoreFileResponse(
                storageKey = request.fileName,
                size = size,
                etag = etag
            )

        } catch (e: Exception) {
            logger.error("Failed to store file: {}", request.fileName, e)
            throw RuntimeException("Failed to store file", e)
        }
    }

    override fun retrieve(storageKey: String): ByteArray? {
        return try {
            val path = rootPath.resolve(storageKey)
            if (Files.exists(path)) Files.readAllBytes(path) else null
        } catch (e: Exception) {
            logger.error("Failed to retrieve file: {}", storageKey, e)
            null
        }
    }

    override fun retrieveAsStream(storageKey: String): InputStream? {
        return try {
            val path = rootPath.resolve(storageKey)
            if (Files.exists(path)) Files.newInputStream(path) else null
        } catch (e: Exception) {
            logger.error("Failed to retrieve file stream: {}", storageKey, e)
            null
        }
    }

    override fun delete(storageKey: String): Boolean {
        return try {
            val path = rootPath.resolve(storageKey)
            val metadataPath = Paths.get("${path}.metadata")

            Files.deleteIfExists(metadataPath)
            Files.deleteIfExists(path)
        } catch (e: Exception) {
            logger.error("Failed to delete file: {}", storageKey, e)
            false
        }
    }

    override fun exists(storageKey: String): Boolean {
        return Files.exists(rootPath.resolve(storageKey))
    }

    override fun generatePresignedUrl(storageKey: String, expirationMinutes: Int): String? {
        // Local files nie obsługują presigned URLs
        // Można zwrócić URL do kontrolera który obsługuje autoryzację
        return "/api/files/download/$storageKey"
    }

    override fun getMetadata(storageKey: String): StorageMetadata? {
        return try {
            val path = rootPath.resolve(storageKey)
            if (!Files.exists(path)) return null

            val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
            val customMetadata = loadMetadata(path)

            StorageMetadata(
                size = attrs.size(),
                contentType = Files.probeContentType(path) ?: "application/octet-stream",
                lastModified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneOffset.UTC),
                etag = generateETag(path),
                customMetadata = customMetadata
            )
        } catch (e: Exception) {
            logger.error("Failed to get metadata for key: {}", storageKey, e)
            null
        }
    }

    override fun copy(sourceKey: String, targetKey: String): Boolean {
        return try {
            val sourcePath = rootPath.resolve(sourceKey)
            val targetPath = rootPath.resolve(targetKey)

            Files.createDirectories(targetPath.parent)
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // Kopiuj też metadane
            val sourceMetadataPath = Paths.get("${sourcePath}.metadata")
            val targetMetadataPath = Paths.get("${targetPath}.metadata")
            if (Files.exists(sourceMetadataPath)) {
                Files.copy(sourceMetadataPath, targetMetadataPath, StandardCopyOption.REPLACE_EXISTING)
            }

            true
        } catch (e: Exception) {
            logger.error("Failed to copy file from {} to {}", sourceKey, targetKey, e)
            false
        }
    }

    override fun move(sourceKey: String, targetKey: String): Boolean {
        return copy(sourceKey, targetKey) && delete(sourceKey)
    }

    private fun generateETag(path: Path): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    private fun saveMetadata(path: Path, metadata: Map<String, String>) {
        if (metadata.isEmpty()) return

        try {
            val metadataPath = Paths.get("${path}.metadata")
            val properties = Properties()
            metadata.forEach { (k, v) -> properties.setProperty(k, v) }

            Files.newOutputStream(metadataPath).use { output ->
                properties.store(output, "File metadata")
            }
        } catch (e: Exception) {
            logger.warn("Failed to save metadata for: {}", path, e)
        }
    }

    private fun loadMetadata(path: Path): Map<String, String> {
        return try {
            val metadataPath = Paths.get("${path}.metadata")
            if (!Files.exists(metadataPath)) return emptyMap()

            val properties = Properties()
            Files.newInputStream(metadataPath).use { input ->
                properties.load(input)
            }

            properties.stringPropertyNames().associateWith { properties.getProperty(it) }
        } catch (e: Exception) {
            logger.warn("Failed to load metadata for: {}", path, e)
            emptyMap()
        }
    }
}