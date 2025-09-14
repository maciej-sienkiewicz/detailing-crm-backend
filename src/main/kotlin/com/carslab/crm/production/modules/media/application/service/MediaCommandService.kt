package com.carslab.crm.production.modules.media.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.dto.MediaUploadResponse
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.domain.service.MediaDomainService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.MediaMetadata
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.infrastructure.request.MediaRequestExtractor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartHttpServletRequest

@Service
@Transactional
class MediaCommandService(
    private val mediaDomainService: MediaDomainService,
    private val securityContext: SecurityContext,
    private val mediaRequestExtractor: MediaRequestExtractor
) {
    private val logger = LoggerFactory.getLogger(MediaCommandService::class.java)

    /**
     * Upload zdjęcia bezpośrednio do pojazdu
     */
    fun uploadMediaForVehicle(vehicleId: String, request: MultipartHttpServletRequest): MediaUploadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to vehicle: {} for company: {}", vehicleId, companyId)

        try {
            val mediaRequest = mediaRequestExtractor.extractMediaRequest(request)

            val metadata = MediaMetadata(
                name = mediaRequest.name,
                description = mediaRequest.description,
                location = mediaRequest.location,
                tags = mediaRequest.tags
            )

            val media = mediaDomainService.uploadMediaForVehicle(
                vehicleId = VehicleId.of(vehicleId.toLong()),
                file = mediaRequest.file,
                metadata = metadata,
                companyId = companyId
            )

            logger.info("Media uploaded successfully to vehicle: {} with ID: {}", vehicleId, media.id.value)

            return MediaUploadResponse(
                id = media.id.value,
                entityId = vehicleId,
                message = "Media uploaded successfully to vehicle"
            )

        } catch (e: Exception) {
            logger.error("Failed to upload media to vehicle: {} for company: {}", vehicleId, companyId, e)
            throw e
        }
    }

    /**
     * Upload zdjęcia do wizyty (zachowanie kompatybilności)
     */
    fun uploadMediaForVisit(visitId: String, request: MultipartHttpServletRequest): MediaUploadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to visit: {} for company: {}", visitId, companyId)

        try {
            val mediaRequest = mediaRequestExtractor.extractMediaRequest(request)

            val metadata = MediaMetadata(
                name = mediaRequest.name,
                description = mediaRequest.description,
                location = mediaRequest.location,
                tags = mediaRequest.tags
            )

            val media = mediaDomainService.uploadMediaForVisit(
                visitId = VisitId.of(visitId),
                file = mediaRequest.file,
                metadata = metadata,
                companyId = companyId
            )

            logger.info("Media uploaded successfully to visit: {} with ID: {}", visitId, media.id.value)

            return MediaUploadResponse(
                id = media.id.value,
                entityId = visitId,
                message = "Media uploaded successfully to visit"
            )

        } catch (e: Exception) {
            logger.error("Failed to upload media to visit: {} for company: {}", visitId, companyId, e)
            throw e
        }
    }

    /**
     * Usuwa media
     */
    fun deleteMedia(mediaId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting media: {} for company: {}", mediaId, companyId)

        try {
            val deleted = mediaDomainService.deleteMedia(MediaId.of(mediaId), companyId)

            if (!deleted) {
                logger.warn("Failed to delete media: {} - not found or access denied", mediaId)
                throw IllegalStateException("Media not found or could not be deleted")
            }

            logger.info("Media deleted successfully: {}", mediaId)

        } catch (e: Exception) {
            logger.error("Failed to delete media: {} for company: {}", mediaId, companyId, e)
            throw e
        }
    }

    /**
     * Usuwa wszystkie media dla pojazdu (dla celów administracyjnych)
     */
    fun deleteAllVehicleMedia(vehicleId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting all media for vehicle: {} for company: {}", vehicleId, companyId)

        try {
            val allMedia = mediaDomainService.getAllVehicleMedia(VehicleId.of(vehicleId.toLong()), companyId)

            allMedia.forEach { media ->
                try {
                    mediaDomainService.deleteMedia(media.id, companyId)
                    logger.debug("Deleted media: {} for vehicle: {}", media.id.value, vehicleId)
                } catch (e: Exception) {
                    logger.warn("Failed to delete media: {} for vehicle: {}", media.id.value, vehicleId, e)
                }
            }

            logger.info("Deleted {} media items for vehicle: {}", allMedia.size, vehicleId)

        } catch (e: Exception) {
            logger.error("Failed to delete all media for vehicle: {} for company: {}", vehicleId, companyId, e)
            throw e
        }
    }

    /**
     * Usuwa wszystkie media dla wizyty (dla celów administracyjnych)
     */
    fun deleteAllVisitMedia(visitId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting all media for visit: {} for company: {}", visitId, companyId)

        try {
            val allMedia = mediaDomainService.getMediaForVisit(VisitId.of(visitId), companyId)

            allMedia.forEach { media ->
                try {
                    mediaDomainService.deleteMedia(media.id, companyId)
                    logger.debug("Deleted media: {} for visit: {}", media.id.value, visitId)
                } catch (e: Exception) {
                    logger.warn("Failed to delete media: {} for visit: {}", media.id.value, visitId, e)
                }
            }

            logger.info("Deleted {} media items for visit: {}", allMedia.size, visitId)

        } catch (e: Exception) {
            logger.error("Failed to delete all media for visit: {} for company: {}", visitId, companyId, e)
            throw e
        }
    }
}