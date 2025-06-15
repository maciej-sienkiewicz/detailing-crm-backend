// src/main/kotlin/com/carslab/crm/modules/visits/infrastructure/storage/ProtocolDocumentStorageService.kt
package com.carslab.crm.modules.visits.infrastructure.storage

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.view.protocol.ProtocolDocumentView
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.modules.visits.infrastructure.persistence.repository.ProtocolDocumentJpaRepository
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolDocumentEntity
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class ProtocolDocumentStorageService(
    private val universalStorageService: UniversalStorageService,
    private val protocolDocumentRepository: ProtocolDocumentJpaRepository,
    private val protocolJpaRepository: ProtocolJpaRepository
) {
    private val logger = LoggerFactory.getLogger(ProtocolDocumentStorageService::class.java)

    /**
     * Przechowuje dokument protokołu
     */
    @Transactional
    fun storeDocument(
        file: MultipartFile,
        protocolId: ProtocolId,
        documentType: String,
        description: String? = null
    ): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Cannot store empty file")
            }

            val companyId = getCurrentCompanyId()
            val protocolIdLong = protocolId.value.toLong()

            // Sprawdź czy protokół istnieje i należy do firmy
            if (!protocolJpaRepository.findByCompanyIdAndId(companyId, protocolIdLong).isPresent) {
                throw RuntimeException("Protocol not found or access denied: ${protocolId.value}")
            }

            // Przechowaj plik używając UniversalStorageService
            val storageId = universalStorageService.storeFile(
                UniversalStoreRequest(
                    file = file,
                    originalFileName = file.originalFilename ?: "document.pdf",
                    contentType = file.contentType ?: "application/pdf",
                    companyId = companyId,
                    entityId = protocolId.value,
                    entityType = "protocol",
                    category = "protocols",
                    subCategory = "documents",
                    description = description,
                    tags = mapOf(
                        "documentType" to documentType,
                        "protocolId" to protocolId.value
                    )
                )
            )

            // Zapisz metadane dokumentu
            val documentEntity = ProtocolDocumentEntity(
                storageId = storageId,
                companyId = companyId,
                protocolId = protocolIdLong,
                originalName = file.originalFilename ?: "document.pdf",
                fileSize = file.size,
                contentType = file.contentType ?: "application/pdf",
                documentType = documentType,
                description = description,
                uploadedBy = getCurrentUserName()
            )

            protocolDocumentRepository.save(documentEntity)

            logger.info("Stored document for protocol ${protocolId.value}: $storageId")
            return storageId

        } catch (e: Exception) {
            logger.error("Failed to store document for protocol ${protocolId.value}", e)
            throw RuntimeException("Failed to store document", e)
        }
    }

    /**
     * Pobiera dane dokumentu
     */
    fun getDocumentData(storageId: String): ByteArray? {
        val companyId = getCurrentCompanyId()

        // Sprawdź uprawnienia
        if (!protocolDocumentRepository.existsByStorageIdAndCompanyId(storageId, companyId)) {
            throw RuntimeException("Document not found or access denied: $storageId")
        }

        return universalStorageService.retrieveFile(storageId)
    }

    /**
     * Pobiera metadane dokumentu
     */
    fun getDocumentMetadata(storageId: String): ProtocolDocumentView? {
        val companyId = getCurrentCompanyId()
        return protocolDocumentRepository.findByStorageIdAndCompanyId(storageId, companyId)?.toDomain()
    }

    /**
     * Pobiera wszystkie dokumenty protokołu
     */
    fun getDocumentsByProtocol(protocolId: ProtocolId): List<ProtocolDocumentView> {
        val companyId = getCurrentCompanyId()
        return protocolDocumentRepository.findByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)
            .map { it.toDomain() }
    }

    /**
     * Usuwa dokument
     */
    @Transactional
    fun deleteDocument(storageId: String): Boolean {
        try {
            val companyId = getCurrentCompanyId()
            val documentEntity = protocolDocumentRepository.findByStorageIdAndCompanyId(storageId, companyId)
                ?: return false

            // Usuń plik z storage
            universalStorageService.deleteFile(storageId)

            // Usuń rekord z bazy
            protocolDocumentRepository.delete(documentEntity)

            logger.info("Deleted document: $storageId")
            return true

        } catch (e: Exception) {
            logger.error("Failed to delete document: $storageId", e)
            return false
        }
    }

    /**
     * Sprawdza czy dokument istnieje
     */
    fun documentExists(storageId: String): Boolean {
        val companyId = getCurrentCompanyId()
        return protocolDocumentRepository.existsByStorageIdAndCompanyId(storageId, companyId)
    }

    /**
     * Pobiera dokumenty po typie
     */
    fun getDocumentsByType(documentType: String): List<ProtocolDocumentView> {
        val companyId = getCurrentCompanyId()
        return protocolDocumentRepository.findByDocumentTypeAndCompanyId(documentType, companyId)
            .map { it.toDomain() }
    }

    /**
     * Usuwa wszystkie dokumenty protokołu
     */
    @Transactional
    fun deleteAllDocumentsByProtocol(protocolId: ProtocolId): Int {
        val companyId = getCurrentCompanyId()
        val documents = protocolDocumentRepository.findByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)

        var deletedCount = 0
        documents.forEach { document ->
            try {
                universalStorageService.deleteFile(document.storageId)
                deletedCount++
            } catch (e: Exception) {
                logger.warn("Failed to delete file from storage: ${document.storageId}", e)
            }
        }

        val deletedFromDb = protocolDocumentRepository.deleteAllByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)
        logger.info("Deleted $deletedFromDb document records for protocol ${protocolId.value}")

        return deletedCount
    }

    /**
     * Generuje URL do pobrania dokumentu (dla S3)
     */
    fun generateDownloadUrl(storageId: String, expirationMinutes: Int = 60): String? {
        val companyId = getCurrentCompanyId()

        if (!protocolDocumentRepository.existsByStorageIdAndCompanyId(storageId, companyId)) {
            throw RuntimeException("Document not found or access denied: $storageId")
        }

        return universalStorageService.generateDownloadUrl(storageId, expirationMinutes)
    }

    /**
     * Pobiera statystyki dokumentów dla firmy
     */
    fun getDocumentStats(): Map<String, Map<String, Any>> {
        val companyId = getCurrentCompanyId()
        val stats = protocolDocumentRepository.getDocumentStats(companyId)

        return stats.associate { row ->
            val documentType = row[0] as String
            val count = row[1] as Long
            val totalSize = row[2] as Long

            documentType to mapOf(
                "count" to count,
                "totalSize" to totalSize,
                "averageSize" to if (count > 0) totalSize / count else 0
            )
        }
    }

    /**
     * Czyści dokumenty osierocone (protokoły zostały usunięte)
     */
    @Transactional
    fun cleanupOrphanedDocuments(cutoffDate: LocalDateTime = LocalDateTime.now().minusDays(30)): Int {
        val companyId = getCurrentCompanyId()
        val orphanedDocuments = protocolDocumentRepository.findOrphanedDocuments(companyId, cutoffDate)

        var cleanedCount = 0
        orphanedDocuments.forEach { document ->
            try {
                universalStorageService.deleteFile(document.storageId)
                protocolDocumentRepository.delete(document)
                cleanedCount++
            } catch (e: Exception) {
                logger.warn("Failed to cleanup orphaned document: ${document.storageId}", e)
            }
        }

        logger.info("Cleaned up $cleanedCount orphaned documents for company $companyId")
        return cleanedCount
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }

    private fun getCurrentUserName(): String {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return "${user.firstName} ${user.lastName}"
    }
}