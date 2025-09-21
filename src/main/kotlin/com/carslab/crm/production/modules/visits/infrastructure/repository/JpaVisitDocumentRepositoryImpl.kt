package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitDocument
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitDocumentRepository
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitDocumentEntity
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaVisitDocumentRepositoryImpl(
    private val documentJpaRepository: VisitDocumentJpaRepository,
    private val visitJpaRepository: VisitJpaRepository,
    private val storageService: UniversalStorageService
) : VisitDocumentRepository {

    @DatabaseMonitored(repository = "visit_document", method = "save", operation = "insert")
    override fun save(document: VisitDocument): VisitDocument {
        val entity = VisitDocumentEntity.Companion.fromDomain(document, document.visitId.value)
        val savedEntity = documentJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @DatabaseMonitored(repository = "visit_document", method = "findByVisitId", operation = "select")
    override fun findByVisitId(visitId: VisitId): List<VisitDocument> {
        return documentJpaRepository.findByVisitId(visitId.value)
            .map { it.toDomain() }
    }

    @DatabaseMonitored(repository = "visit_document", method = "findById", operation = "select")
    override fun findById(documentId: String): VisitDocument? {
        return documentJpaRepository.findById(documentId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @DatabaseMonitored(repository = "visit_document", method = "existsVisitByIdAndCompanyId", operation = "select")
    override fun existsVisitByIdAndCompanyId(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.existsByIdAndCompanyId(visitId.value, companyId)
    }

    @DatabaseMonitored(repository = "visit_document", method = "deleteById", operation = "delete")
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

    @DatabaseMonitored(repository = "visit_document", method = "getFileData", operation = "select")
    override fun getFileData(documentId: String): ByteArray? {
        return try {
            storageService.retrieveFile(documentId)
        } catch (e: Exception) {
            null
        }
    }
}