package com.carslab.crm.production.modules.media.infrastructure.repository

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.domain.repository.MediaRepository
import com.carslab.crm.production.modules.media.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.media.infrastructure.mapper.toEntity
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaMediaRepositoryImpl(
    private val jpaRepository: MediaJpaRepository
) : MediaRepository {

    private val logger = LoggerFactory.getLogger(JpaMediaRepositoryImpl::class.java)

    @DatabaseMonitored(repository = "media", method = "save", operation = "insert")
    override fun save(media: Media): Media {
        logger.debug("Saving media: {} for company: {}", media.id.value, media.companyId)

        val entity = media.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Media saved successfully: {}", savedEntity.id)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findById", operation = "select")
    override fun findById(mediaId: MediaId): Media? {
        logger.debug("Finding media by ID: {}", mediaId.value)

        return jpaRepository.findById(mediaId.value)
            .map { it.toDomain() }
            .orElse(null)
            .also { result ->
                if (result == null) {
                    logger.debug("Media not found: {}", mediaId.value)
                } else {
                    logger.debug("Media found: {}", mediaId.value)
                }
            }
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByVisitId", operation = "select")
    override fun findByVisitId(visitId: VisitId): List<Media> {
        logger.debug("Finding media for visit: {}", visitId.value)

        val media = jpaRepository.findByVisitId(visitId.value)
            .map { it.toDomain() }

        logger.debug("Found {} media items for visit: {}", media.size, visitId.value)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByVehicleId", operation = "select")
    override fun findByVehicleId(vehicleId: VehicleId, companyId: Long): List<Media> {
        logger.debug("Finding direct media for vehicle: {} and company: {}", vehicleId.value, companyId)

        val media = jpaRepository.findByVehicleIdDirect(vehicleId.value, companyId)
            .map { it.toDomain() }

        logger.debug("Found {} direct media items for vehicle: {}", media.size, vehicleId.value)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findAllVehicleMedia", operation = "select")
    override fun findAllVehicleMedia(vehicleId: VehicleId, companyId: Long): List<Media> {
        logger.debug("Finding all media for vehicle: {} and company: {}", vehicleId.value, companyId)

        val media = jpaRepository.findAllByVehicleId(vehicleId.value, companyId)
            .map { it.toDomain() }

        logger.debug("Found {} total media items for vehicle: {}", media.size, vehicleId.value)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByContextAndEntityId", operation = "select")
    override fun findByContextAndEntityId(context: MediaContext, entityId: Long, companyId: Long): List<Media> {
        logger.debug("Finding media by context: {} and entity: {} for company: {}", context, entityId, companyId)

        val media = jpaRepository.findByContextAndEntityId(context, entityId, companyId)
            .map { it.toDomain() }

        logger.debug("Found {} media items for context: {} and entity: {}", media.size, context, entityId)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByContextAndCompanyId", operation = "select")
    override fun findByContextAndCompanyId(context: MediaContext, companyId: Long): List<Media> {
        logger.debug("Finding media by context: {} for company: {}", context, companyId)

        val media = jpaRepository.findByContextAndCompanyId(context, companyId)
            .map { it.toDomain() }

        logger.debug("Found {} media items for context: {}", media.size, context)
        return media
    }

    @DatabaseMonitored(repository = "media", method = "deleteById", operation = "delete")
    override fun deleteById(mediaId: MediaId): Boolean {
        logger.debug("Deleting media: {}", mediaId.value)

        return if (jpaRepository.existsById(mediaId.value)) {
            jpaRepository.deleteById(mediaId.value)
            logger.info("Media deleted successfully: {}", mediaId.value)
            true
        } else {
            logger.warn("Media not found for deletion: {}", mediaId.value)
            false
        }
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "existsByIdAndCompanyId", operation = "select")
    override fun existsByIdAndCompanyId(mediaId: MediaId, companyId: Long): Boolean {
        return jpaRepository.existsByIdAndCompanyId(mediaId.value, companyId)
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByCompanyId", operation = "select")
    override fun findByCompanyId(companyId: Long): List<Media> {
        logger.debug("Finding all media for company: {}", companyId)

        val media = jpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }

        logger.debug("Found {} media items for company: {}", media.size, companyId)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "findByCompanyIdWithPagination", operation = "select")
    override fun findByCompanyId(companyId: Long, limit: Int, offset: Int): List<Media> {
        logger.debug("Finding media for company: {} with limit: {} and offset: {}", companyId, limit, offset)

        val media = jpaRepository.findByCompanyIdWithPagination(companyId, limit, offset)
            .map { it.toDomain() }

        logger.debug("Found {} media items for company: {}", media.size, companyId)
        return media
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "media", method = "countByCompanyId", operation = "select")
    override fun countByCompanyId(companyId: Long): Long {
        return jpaRepository.countByCompanyId(companyId)
    }
}