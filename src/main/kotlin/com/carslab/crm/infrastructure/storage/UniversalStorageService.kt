// src/main/kotlin/com/carslab/crm/infrastructure/storage/UniversalStorageService.kt
package com.carslab.crm.infrastructure.storage

import com.carslab.crm.finances.domain.FileMetadata
import com.carslab.crm.infrastructure.storage.entity.FileMetadataEntity
import com.carslab.crm.infrastructure.storage.key.StorageKeyGenerator
import com.carslab.crm.infrastructure.storage.key.StorageKeyRequest
import com.carslab.crm.infrastructure.storage.provider.*
import com.carslab.crm.infrastructure.storage.repository.FileMetadataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class UniversalStorageService(
    private val storageProvider: StorageProvider,
    private val keyGenerator: StorageKeyGenerator,
    private val fileMetadataRepository: FileMetadataRepository
) {
    private val logger = LoggerFactory.getLogger(UniversalStorageService::class.java)

    @Transactional
    fun storeFile(request: UniversalStoreRequest): String {
        try {
            val storageId = UUID.randomUUID().toString()
            val extension = request.originalFileName.substringAfterLast('.', "")
            val fileName = if (extension.isNotEmpty()) "$storageId.$extension" else storageId

            val storageKey = keyGenerator.generateKey(
                StorageKeyRequest(
                    companyId = request.companyId,
                    category = request.category,
                    subCategory = request.subCategory,
                    fileName = fileName,
                    date = request.date
                )
            )

            val storeRequest = StoreFileRequest(
                file = request.file,
                fileName = storageKey,
                metadata = mapOf(
                    "entityId" to request.entityId,
                    "entityType" to request.entityType,
                    "companyId" to request.companyId.toString(),
                    "originalName" to request.originalFileName
                ),
                tags = request.tags ?: emptyMap()
            )

            val response = storageProvider.store(storeRequest)

            // Zapisz metadane w bazie danych
            val metadata = FileMetadataEntity(
                storageId = storageId,
                originalName = request.originalFileName,
                filePath = storageKey,
                fileSize = response.size,
                contentType = request.file?.contentType ?: request.contentType,
                companyId = request.companyId,
                entityId = request.entityId,
                entityType = request.entityType,
                category = request.category,
                subCategory = request.subCategory,
                description = request.description
            )

            fileMetadataRepository.save(metadata)

            logger.info("Stored file: {} -> {} (provider: {})",
                request.originalFileName, storageKey, storageProvider.javaClass.simpleName)

            return storageId

        } catch (e: Exception) {
            logger.error("Failed to store file: {}", request.originalFileName, e)
            throw RuntimeException("Failed to store file", e)
        }
    }

    fun retrieveFile(storageId: String): ByteArray? {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return null
        return storageProvider.retrieve(metadata.filePath)
    }

    fun retrieveFileAsStream(storageId: String): java.io.InputStream? {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return null
        return storageProvider.retrieveAsStream(metadata.filePath)
    }

    @Transactional
    fun deleteFile(storageId: String): Boolean {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return false

        val deleted = storageProvider.delete(metadata.filePath)
        if (deleted) {
            fileMetadataRepository.deleteById(storageId)
        }

        return deleted
    }

    fun generateDownloadUrl(storageId: String, expirationMinutes: Int = 60): String? {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return null
        return storageProvider.generatePresignedUrl(metadata.filePath, expirationMinutes)
    }

    fun getFileMetadata(storageId: String): FileMetadata? {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return null
        return FileMetadata(
            storageId = metadata.storageId,
            originalName = metadata.originalName,
            fileSize = metadata.fileSize,
            contentType = metadata.contentType,
            createdAt = metadata.createdAt,
            description = metadata.description
        )
    }

    fun exists(storageId: String): Boolean {
        val metadata = fileMetadataRepository.findById(storageId).orElse(null) ?: return false
        return storageProvider.exists(metadata.filePath)
    }
}

data class UniversalStoreRequest(
    val file: MultipartFile? = null,
    val originalFileName: String,
    val contentType: String = "application/octet-stream",
    val companyId: Long,
    val entityId: String,
    val entityType: String,
    val category: String,
    val subCategory: String? = null,
    val description: String? = null,
    val date: LocalDate = LocalDate.now(),
    val tags: Map<String, String>? = null
)