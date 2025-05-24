package com.carslab.crm.domain.finances.documents

import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Serwis do przechowywania plików dokumentów finansowych.
 */
@Service
class UnifiedDocumentStorageService(
    @Value("\${file.upload-dir:uploads/documents}")
    private val documentsDir: String
) {
    private val logger = LoggerFactory.getLogger(UnifiedDocumentStorageService::class.java)
    private val rootLocation: Path = Paths.get(documentsDir)

    init {
        try {
            Files.createDirectories(rootLocation)
            logger.info("Document storage directory initialized at: {}", rootLocation.toAbsolutePath())
        } catch (e: IOException) {
            logger.error("Could not initialize document storage", e)
            throw RuntimeException("Could not initialize document storage location", e)
        }
    }

    /**
     * Przechowuje plik dokumentu i zwraca identyfikator przechowywania.
     */
    fun storeDocumentFile(file: MultipartFile, documentId: UnifiedDocumentId): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Failed to store empty file")
            }

            // Tworzenie unikalnego identyfikatora dla pliku
            val storageId = UUID.randomUUID().toString()

            // Tworzenie katalogu dla dokumentu, jeśli nie istnieje
            val documentDir = rootLocation.resolve(documentId.value)
            Files.createDirectories(documentDir)

            // Przechowywanie pliku z wygenerowanym ID jako nazwą pliku (zachowując oryginalne rozszerzenie)
            val originalFilename = file.originalFilename ?: "unknown"
            val extension = originalFilename.substringAfterLast('.', "")
            val filename = "$storageId.$extension"
            val targetPath = documentDir.resolve(filename)

            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            logger.info("Stored document file for document {}: {}", documentId.value, targetPath)
            return storageId
        } catch (e: Exception) {
            logger.error("Failed to store document file", e)
            throw RuntimeException("Failed to store document file", e)
        }
    }

    /**
     * Przechowuje wiele plików dokumentu.
     */
    fun storeDocumentFiles(files: List<MultipartFile>, documentId: UnifiedDocumentId): List<String> {
        return files.map { file -> storeDocumentFile(file, documentId) }
    }

    /**
     * Pobiera plik dokumentu.
     */
    fun getDocumentFile(storageId: String): ByteArray {
        try {
            // Znajdujemy plik na podstawie identyfikatora przechowywania
            val file = findDocumentFileByStorageId(storageId)
            if (file != null) {
                return Files.readAllBytes(file)
            }
            throw RuntimeException("Document file not found: $storageId")
        } catch (e: Exception) {
            logger.error("Failed to read document file", e)
            throw RuntimeException("Failed to read document file", e)
        }
    }

    /**
     * Pobiera plik dokumentu jako zasób.
     */
    fun getDocumentFileAsResource(storageId: String): Resource {
        try {
            val file = findDocumentFileByStorageId(storageId)
                ?: throw RuntimeException("Document file not found: $storageId")

            val resource = UrlResource(file.toUri())
            if (resource.exists() || resource.isReadable) {
                return resource
            } else {
                throw RuntimeException("Could not read document file: $storageId")
            }
        } catch (e: MalformedURLException) {
            logger.error("Failed to read document file", e)
            throw RuntimeException("Could not read document file: $storageId", e)
        }
    }

    /**
     * Usuwa plik dokumentu.
     */
    fun deleteDocumentFile(storageId: String): Boolean {
        try {
            val file = findDocumentFileByStorageId(storageId)
            if (file != null) {
                return Files.deleteIfExists(file)
            }
            logger.warn("Document file not found for deletion: {}", storageId)
            return false
        } catch (e: Exception) {
            logger.error("Failed to delete document file", e)
            throw RuntimeException("Failed to delete document file", e)
        }
    }

    /**
     * Usuwa wszystkie pliki powiązane z dokumentem.
     */
    fun deleteDocumentFiles(documentId: UnifiedDocumentId): Boolean {
        try {
            val documentDir = rootLocation.resolve(documentId.value)
            if (Files.exists(documentDir)) {
                FileSystemUtils.deleteRecursively(documentDir)
                return true
            }
            return false
        } catch (e: Exception) {
            logger.error("Failed to delete document files for document: {}", documentId.value, e)
            throw RuntimeException("Failed to delete document files", e)
        }
    }

    /**
     * Pobiera informacje o pliku (bez pobierania zawartości).
     */
    fun getDocumentFileInfo(storageId: String): DocumentFileInfo? {
        try {
            val file = findDocumentFileByStorageId(storageId)
            if (file != null) {
                return DocumentFileInfo(
                    storageId = storageId,
                    name = file.fileName.toString(),
                    size = Files.size(file),
                    contentType = Files.probeContentType(file) ?: "application/octet-stream",
                    lastModified = Files.getLastModifiedTime(file).toInstant()
                )
            }
            return null
        } catch (e: Exception) {
            logger.error("Failed to get document file info", e)
            return null
        }
    }

    /**
     * Znajduje plik dokumentu na podstawie identyfikatora przechowywania.
     */
    private fun findDocumentFileByStorageId(storageId: String): Path? {
        try {
            // Przeszukujemy wszystkie podkatalogi, aby znaleźć plik dokumentu
            val documentFiles = Files.walk(rootLocation)
                .filter { path ->
                    Files.isRegularFile(path) &&
                            path.fileName.toString().startsWith("$storageId.")
                }
                .toList()

            return if (documentFiles.isNotEmpty()) {
                documentFiles[0]
            } else {
                null
            }
        } catch (e: IOException) {
            logger.error("Failed to search for document file", e)
            return null
        }
    }

    /**
     * Usuwa wszystkie pliki dokumentów.
     */
    fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
        Files.createDirectories(rootLocation)
    }

    /**
     * Sprawdza dostępność miejsca na dysku.
     */
    fun checkDiskSpace(): DiskSpaceInfo {
        return try {
            val store = Files.getFileStore(rootLocation)
            DiskSpaceInfo(
                totalSpace = store.totalSpace,
                freeSpace = store.usableSpace,
                usedSpace = store.totalSpace - store.usableSpace
            )
        } catch (e: Exception) {
            logger.error("Failed to check disk space", e)
            DiskSpaceInfo(0, 0, 0)
        }
    }

    /**
     * Pobiera statystyki przechowywania.
     */
    fun getStorageStatistics(): StorageStatistics {
        return try {
            var totalFiles = 0L
            var totalSize = 0L

            Files.walk(rootLocation)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    totalFiles++
                    totalSize += Files.size(file)
                }

            StorageStatistics(
                totalFiles = totalFiles,
                totalSize = totalSize,
                averageFileSize = if (totalFiles > 0) totalSize / totalFiles else 0
            )
        } catch (e: Exception) {
            logger.error("Failed to get storage statistics", e)
            StorageStatistics(0, 0, 0)
        }
    }
}

/**
 * Informacje o pliku dokumentu.
 */
data class DocumentFileInfo(
    val storageId: String,
    val name: String,
    val size: Long,
    val contentType: String,
    val lastModified: java.time.Instant
)

/**
 * Informacje o miejscu na dysku.
 */
data class DiskSpaceInfo(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
)

/**
 * Statystyki przechowywania.
 */
data class StorageStatistics(
    val totalFiles: Long,
    val totalSize: Long,
    val averageFileSize: Long
)