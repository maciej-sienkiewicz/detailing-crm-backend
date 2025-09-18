package com.carslab.crm.production.modules.media.domain.service

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.domain.repository.MediaRepository
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleAccessValidator
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.MediaMetadata
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.details.media.MediaFileValidator
import com.carslab.crm.production.modules.visits.domain.service.details.media.MediaStorageService
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class MediaDomainService(
    private val mediaRepository: MediaRepository,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val mediaStorageService: MediaStorageService,
    private val mediaFileValidator: MediaFileValidator,
    private val visitRepository: VisitRepository
) {
    private val logger = LoggerFactory.getLogger(MediaDomainService::class.java)

    fun uploadMediaForVehicle(
        vehicleId: VehicleId,
        file: MultipartFile,
        metadata: MediaMetadata,
        companyId: Long
    ): Media {
        logger.info("Uploading media for vehicle: {} for company: {}", vehicleId.value, companyId)

        val vehicle = vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        mediaFileValidator.validateFile(file)

        val storageId = mediaStorageService.storeMediaFile(
            com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand(
                visitId = VisitId.of(1L),
                file = file,
                metadata = metadata
            ),
            companyId,
            entityType = "vehicle",
            category = "vehicle",
            id = vehicleId.value.toString(),
        )

        val media = Media.createForVehicle(
            vehicleId = vehicleId,
            companyId = companyId,
            name = metadata.name,
            description = metadata.description,
            location = metadata.location,
            tags = metadata.tags,
            type = MediaType.PHOTO,
            size = file.size,
            contentType = file.contentType ?: "image/jpeg",
            storageId = storageId
        )

        val savedMedia = mediaRepository.save(media)
        logger.info("Media uploaded successfully for vehicle: {} with ID: {}", vehicleId.value, savedMedia.id.value)

        return savedMedia
    }

    fun uploadMediaForVisit(
        visitId: VisitId,
        file: MultipartFile,
        metadata: MediaMetadata,
        companyId: Long
    ): Media {
        logger.info("Uploading media for visit: {} for company: {}", visitId.value, companyId)

        val visit = visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

        mediaFileValidator.validateFile(file)

        val storageId = mediaStorageService.storeMediaFile(
            com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand(
                visitId = visitId,
                file = file,
                metadata = metadata
            ),
            companyId
        )

        val media = Media.createForVisit(
            visitId = visitId,
            vehicleId = visit.vehicleId,
            companyId = companyId,
            name = metadata.name,
            description = metadata.description,
            location = metadata.location,
            tags = metadata.tags,
            type = MediaType.PHOTO,
            size = file.size,
            contentType = file.contentType ?: "image/jpeg",
            storageId = storageId
        )

        val savedMedia = mediaRepository.save(media)
        logger.info("Media uploaded successfully for visit: {} with ID: {}", visitId.value, savedMedia.id.value)

        return savedMedia
    }

    fun getAllVehicleMedia(vehicleId: VehicleId, companyId: Long): List<Media> {
        logger.debug("Getting all media for vehicle: {} and company: {}", vehicleId.value, companyId)

        vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        val allMedia = mediaRepository.findAllVehicleMedia(vehicleId, companyId)

        logger.debug("Found {} media items for vehicle: {}", allMedia.size, vehicleId.value)
        return allMedia
    }

    fun getMediaForVisit(visitId: VisitId, companyId: Long): List<Media> {
        logger.debug("Getting media for visit: {}", visitId.value)

        visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

        return mediaRepository.findByVisitId(visitId)
    }

    fun getAllMediaForCompany(companyId: Long): List<Media> {
        logger.debug("Getting all media for company: {}", companyId)

        return mediaRepository.findByCompanyId(companyId)
    }

    fun getAllMediaByContextForCompany(context: MediaContext, companyId: Long): List<Media> {
        logger.debug("Getting all media by context: {} for company: {}", context, companyId)

        return mediaRepository.findByContextAndCompanyId(context, companyId)
    }

    fun getMediaData(mediaId: MediaId, companyId: Long): ByteArray? {
        logger.debug("Getting media data for: {}", mediaId.value)

        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        return mediaStorageService.retrieveMediaData(mediaId.value)
    }

    fun deleteMedia(mediaId: MediaId, companyId: Long): Boolean {
        logger.info("Deleting media: {} for company: {}", mediaId.value, companyId)

        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        mediaStorageService.deleteMediaFile(mediaId.value)

        val deleted = mediaRepository.deleteById(mediaId)

        if (deleted) {
            logger.info("Media deleted successfully: {}", mediaId.value)
        } else {
            logger.warn("Failed to delete media: {}", mediaId.value)
        }

        return deleted
    }

    fun existsMedia(mediaId: MediaId, companyId: Long): Boolean {
        return mediaRepository.existsByIdAndCompanyId(mediaId, companyId)
    }

    fun getMediaById(mediaId: MediaId, companyId: Long): Media {
        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        return media
    }

    fun updateMediaTags(mediaId: MediaId, tags: List<String>, companyId: Long): Boolean {
        logger.info("Updating tags for media: {} for company: {}", mediaId.value, companyId)

        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        // Walidacja tagów
        require(tags.size <= 20) { "Too many tags (max 20)" }
        require(tags.all { it.isNotBlank() && it.length <= 50 }) {
            "All tags must be non-blank and max 50 characters"
        }

        val updatedMedia = media.copy(
            tags = tags.map { it.trim() }.distinct(),
            updatedAt = LocalDateTime.now()
        )

        mediaRepository.save(updatedMedia)

        logger.info("Media tags updated successfully: {} with {} tags", mediaId.value, tags.size)
        return true
    }
}