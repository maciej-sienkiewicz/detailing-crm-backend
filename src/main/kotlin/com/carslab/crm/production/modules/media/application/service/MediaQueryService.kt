package com.carslab.crm.production.modules.media.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.dto.GalleryMediaResponse
import com.carslab.crm.production.modules.media.application.dto.MediaFileResponse
import com.carslab.crm.production.modules.media.application.dto.MediaResponse
import com.carslab.crm.production.modules.media.application.dto.MediaStatsResponse
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.domain.service.MediaDomainService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MediaQueryService(
    private val mediaDomainService: MediaDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(MediaQueryService::class.java)

    /**
     * Pobiera wszystkie zdjęcia pojazdu (bezpośrednio przypisane + z wizyt)
     */
    fun getAllVehicleMedia(vehicleId: String): List<MediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting all media for vehicle: {} and company: {}", vehicleId, companyId)

        return try {
            val media = mediaDomainService.getAllVehicleMedia(VehicleId.of(vehicleId.toLong()), companyId)
            val responses = media.map { MediaResponse.from(it) }

            logger.debug("Retrieved {} media items for vehicle: {}", responses.size, vehicleId)
            responses

        } catch (e: Exception) {
            logger.error("Failed to get media for vehicle: {} for company: {}", vehicleId, companyId, e)
            throw e
        }
    }

    /**
     * Pobiera zdjęcia dla wizyty (kompatybilność wsteczna)
     */
    fun getVisitMedia(visitId: String): List<MediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting media for visit: {} and company: {}", visitId, companyId)

        return try {
            val media = mediaDomainService.getMediaForVisit(VisitId.of(visitId), companyId)
            val responses = media.map { MediaResponse.from(it) }

            logger.debug("Retrieved {} media items for visit: {}", responses.size, visitId)
            responses

        } catch (e: Exception) {
            logger.error("Failed to get media for visit: {} for company: {}", visitId, companyId, e)
            throw e
        }
    }

    /**
     * Pobiera dane pliku media
     */
    fun getMediaFile(mediaId: String): ByteArray? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting media file: {} for company: {}", mediaId, companyId)

        return try {
            mediaDomainService.getMediaData(MediaId.of(mediaId), companyId)
        } catch (e: Exception) {
            logger.error("Failed to get media file: {} for company: {}", mediaId, companyId, e)
            null
        }
    }

    /**
     * Pobiera zdjęcie z metadanymi (kompatybilność z istniejącym API)
     */
    fun getImageWithMetadata(mediaId: String): GetMediaQuery? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting image with metadata: {} for company: {}", mediaId, companyId)

        return try {
            val media = mediaDomainService.getMediaById(MediaId.of(mediaId), companyId)
            val data = mediaDomainService.getMediaData(media.id, companyId) ?: return null

            GetMediaQuery(
                data = data,
                contentType = media.contentType,
                originalName = media.name,
                size = media.size
            )

        } catch (e: Exception) {
            logger.error("Failed to get image with metadata: {} for company: {}", mediaId, companyId, e)
            null
        }
    }

    /**
     * Pobiera media z metadanymi dla download
     */
    fun getMediaWithMetadata(mediaId: String): MediaFileResponse? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting media with metadata: {} for company: {}", mediaId, companyId)

        return try {
            val media = mediaDomainService.getMediaById(MediaId.of(mediaId), companyId)
            val data = mediaDomainService.getMediaData(media.id, companyId) ?: return null

            MediaFileResponse(
                data = data,
                contentType = media.contentType,
                originalName = media.name,
                size = media.size
            )

        } catch (e: Exception) {
            logger.error("Failed to get media with metadata: {} for company: {}", mediaId, companyId, e)
            null
        }
    }

    /**
     * Pobiera media dla galerii z dodatkową informacją kontekstową
     */
    fun getGalleryMediaForVehicle(vehicleId: String): List<GalleryMediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting gallery media for vehicle: {} and company: {}", vehicleId, companyId)

        return try {
            val media = mediaDomainService.getAllVehicleMedia(VehicleId.of(vehicleId.toLong()), companyId)

            media.map { mediaItem ->
                val vehicleInfo = "Vehicle ID: $vehicleId"
                val visitInfo = mediaItem.visitId?.let { "Visit ID: ${it.value}" }

                GalleryMediaResponse.from(mediaItem, vehicleInfo, visitInfo)
            }

        } catch (e: Exception) {
            logger.error("Failed to get gallery media for vehicle: {} for company: {}", vehicleId, companyId, e)
            emptyList()
        }
    }

    /**
     * Sprawdza czy media istnieje
     */
    fun existsMedia(mediaId: String): Boolean {
        val companyId = securityContext.getCurrentCompanyId()
        return mediaDomainService.existsMedia(MediaId.of(mediaId), companyId)
    }

    /**
     * Pobiera media po ID
     */
    fun getMediaById(mediaId: String): MediaResponse? {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting media by ID: {} for company: {}", mediaId, companyId)

        return try {
            val media = mediaDomainService.getMediaById(MediaId.of(mediaId), companyId)
            MediaResponse.from(media)

        } catch (e: Exception) {
            logger.error("Failed to get media by ID: {} for company: {}", mediaId, companyId, e)
            null
        }
    }

    /**
     * Pobiera statystyki media dla firmy (dla przyszłego użytku)
     */
    fun getMediaStats(): MediaStatsResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting media stats for company: {}", companyId)

        // TODO: Implementacja statystyk po dodaniu odpowiednich metod w repository
        return MediaStatsResponse(
            totalCount = 0L,
            totalSize = 0L,
            vehicleMediaCount = 0L,
            visitMediaCount = 0L,
            byType = emptyMap()
        )
    }
}