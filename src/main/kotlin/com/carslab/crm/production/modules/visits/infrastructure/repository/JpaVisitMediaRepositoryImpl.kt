package com.carslab.crm.production.modules.visits.infrastructure.repository

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitMediaRepository
import com.carslab.crm.production.modules.visits.infrastructure.entity.VisitMediaEntity
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaVisitMediaRepositoryImpl(
    private val mediaJpaRepository: VisitMediaJpaRepository,
    private val visitJpaRepository: VisitJpaRepository,
    private val storageService: UniversalStorageService
) : VisitMediaRepository {

    @DatabaseMonitored(repository = "visit_media", method = "save", operation = "insert")
    override fun save(media: VisitMedia): VisitMedia {
        val entity = VisitMediaEntity.Companion.fromDomain(media)
        val savedEntity = mediaJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @DatabaseMonitored(repository = "visit_media", method = "findByVisitId", operation = "select")
    override fun findByVisitId(visitId: VisitId): List<VisitMedia> {
        return mediaJpaRepository.findByVisitId(visitId.value)
            .map { it.toDomain() }
    }

    @DatabaseMonitored(repository = "visit_media", method = "findById", operation = "select")
    override fun findById(mediaId: String): VisitMedia? {
        return mediaJpaRepository.findById(mediaId)
            .map { it.toDomain() }
            .orElse(null)
    }

    @DatabaseMonitored(repository = "visit_media", method = "existsVisitByIdAndCompanyId", operation = "select")
    override fun existsVisitByIdAndCompanyId(visitId: VisitId, companyId: Long): Boolean {
        return visitJpaRepository.existsByIdAndCompanyId(visitId.value, companyId)
    }

    @DatabaseMonitored(repository = "visit_media", method = "deleteById", operation = "delete")
    override fun deleteById(mediaId: String): Boolean {
        return if (mediaJpaRepository.existsById(mediaId)) {
            try {
                storageService.deleteFile(mediaId)
            } catch (e: Exception) {
                // Log but don't fail if storage deletion fails
            }
            mediaJpaRepository.deleteById(mediaId)
            true
        } else {
            false
        }
    }

    @DatabaseMonitored(repository = "visit_media", method = "getFileData", operation = "select")
    override fun getFileData(mediaId: String): ByteArray? {
        return try {
            storageService.retrieveFile(mediaId)
        } catch (e: Exception) {
            null
        }
    }
}