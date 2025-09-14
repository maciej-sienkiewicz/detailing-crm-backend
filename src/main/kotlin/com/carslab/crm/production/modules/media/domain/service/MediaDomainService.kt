package com.carslab.crm.production.modules.media.domain.service

import com.carslab.crm.production.modules.media.domain.model.Media
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

@Service
class MediaDomainService(
    private val mediaRepository: MediaRepository,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val mediaStorageService: MediaStorageService,
    private val mediaFileValidator: MediaFileValidator,
    private val visitRepository: VisitRepository
) {
    private val logger = LoggerFactory.getLogger(MediaDomainService::class.java)

    /**
     * Upload zdjęcia bezpośrednio do pojazdu
     */
    fun uploadMediaForVehicle(
        vehicleId: VehicleId,
        file: MultipartFile,
        metadata: MediaMetadata,
        companyId: Long
    ): Media {
        logger.info("Uploading media for vehicle: {} for company: {}", vehicleId.value, companyId)

        // Walidacja dostępu do pojazdu
        val vehicle = vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        // Walidacja pliku
        mediaFileValidator.validateFile(file)

        // Upload do storage
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

        // Utworzenie Media z context = VEHICLE
        val media = Media.createForVehicle(
            vehicleId = vehicleId,
            companyId = companyId,
            name = metadata.name,
            description = metadata.description,
            location = metadata.location,
            tags = metadata.tags,
            type = MediaType.PHOTO, // Assuming photos for now
            size = file.size,
            contentType = file.contentType ?: "image/jpeg",
            storageId = storageId
        )

        val savedMedia = mediaRepository.save(media)
        logger.info("Media uploaded successfully for vehicle: {} with ID: {}", vehicleId.value, savedMedia.id.value)

        return savedMedia
    }

    /**
     * Upload zdjęcia do wizyty z automatycznym przypisaniem do pojazdu
     */
    fun uploadMediaForVisit(
        visitId: VisitId,
        file: MultipartFile,
        metadata: MediaMetadata,
        companyId: Long
    ): Media {
        logger.info("Uploading media for visit: {} for company: {}", visitId.value, companyId)

        // Pobranie wizyty i vehicleId
        val visit = visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

        // Walidacja pliku
        mediaFileValidator.validateFile(file)

        // Upload do storage
        val storageId = mediaStorageService.storeMediaFile(
            com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand(
                visitId = visitId,
                file = file,
                metadata = metadata
            ),
            companyId
        )

        // Utworzenie Media z context = VISIT i wypełnionym vehicle_id
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

    /**
     * Pobiera wszystkie zdjęcia pojazdu (bezpośrednio przypisane + z wizyt)
     */
    fun getAllVehicleMedia(vehicleId: VehicleId, companyId: Long): List<Media> {
        logger.debug("Getting all media for vehicle: {} and company: {}", vehicleId.value, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)

        // Zwraca wszystkie zdjęcia pojazdu:
        // 1. Bezpośrednio przypisane (context = VEHICLE, entity_id = vehicleId)
        // 2. Z wizyt (context = VISIT, vehicle_id = vehicleId)
        val allMedia = mediaRepository.findAllVehicleMedia(vehicleId, companyId)

        logger.debug("Found {} media items for vehicle: {}", allMedia.size, vehicleId.value)
        return allMedia
    }

    /**
     * Pobiera zdjęcia tylko dla wizyty
     */
    fun getMediaForVisit(visitId: VisitId, companyId: Long): List<Media> {
        logger.debug("Getting media for visit: {}", visitId.value)

        // Sprawdzenie czy wizyta istnieje i należy do firmy
        visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

        return mediaRepository.findByVisitId(visitId)
    }

    /**
     * Pobiera dane pliku media
     */
    fun getMediaData(mediaId: MediaId, companyId: Long): ByteArray? {
        logger.debug("Getting media data for: {}", mediaId.value)

        // Sprawdzenie uprawnień
        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        return mediaStorageService.retrieveMediaData(mediaId.value)
    }

    /**
     * Usuwa media
     */
    fun deleteMedia(mediaId: MediaId, companyId: Long): Boolean {
        logger.info("Deleting media: {} for company: {}", mediaId.value, companyId)

        // Sprawdzenie uprawnień
        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        // Usuń z storage
        mediaStorageService.deleteMediaFile(mediaId.value)

        // Usuń z bazy
        val deleted = mediaRepository.deleteById(mediaId)

        if (deleted) {
            logger.info("Media deleted successfully: {}", mediaId.value)
        } else {
            logger.warn("Failed to delete media: {}", mediaId.value)
        }

        return deleted
    }

    /**
     * Sprawdza czy media istnieje i należy do firmy
     */
    fun existsMedia(mediaId: MediaId, companyId: Long): Boolean {
        return mediaRepository.existsByIdAndCompanyId(mediaId, companyId)
    }

    /**
     * Pobiera media po ID z walidacją uprawnień
     */
    fun getMediaById(mediaId: MediaId, companyId: Long): Media {
        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        return media
    }
}