package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.production.modules.visits.domain.model.VisitDocument
import com.carslab.crm.production.modules.visits.domain.model.VisitId
import com.carslab.crm.production.modules.visits.domain.repository.VisitDocumentRepository
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitDocumentEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaVisitDocumentRepositoryImpl(
    private val documentJpaRepository: VisitDocumentJpaRepository,
    private val storageService: UniversalStorageService
) : VisitDocumentRepository {

    override fun save(document: VisitDocument): VisitDocument {
        val entity = VisitDocumentEntity.Companion.fromDomain(document, document.visitId.value)
        val savedEntity = documentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByVisitId(visitId: VisitId): List<VisitDocument> {
        return documentJpaRepository.findByVisitId(visitId.value)
            .map { it.toDomain() }
    }

    override fun findById(documentId: String): VisitDocument? {
        return documentJpaRepository.findById(documentId)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun deleteById(documentId: String): Boolean {
        return if (documentJpaRepository.existsById(documentId)) {
            try {
                storageService.deleteFile(documentId)
            } catch (e: Exception) {
                // Log but don't fail if storage deletion fails
            }
            documentJpaRepository.deleteById(documentId)
            true
        } else {
            false
        }
    }

    override fun getFileData(documentId: String): ByteArray? {
        return try {
            storageService.retrieveFile(documentId)
        } catch (e: Exception) {
            null
        }
    }
}