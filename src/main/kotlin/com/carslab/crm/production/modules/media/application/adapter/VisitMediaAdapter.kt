package com.carslab.crm.production.modules.media.application.adapter

import com.carslab.crm.production.modules.media.application.service.MediaCommandService
import com.carslab.crm.production.modules.media.application.service.MediaQueryService
import com.carslab.crm.production.modules.visits.application.dto.MediaUploadResponse
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.application.dto.from
import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * Adapter dla zachowania kompatybilności API wizyt
 * Deleguje wywołania do nowego systemu Media
 */
@Component
class VisitMediaAdapter(
    private val mediaCommandService: MediaCommandService,
    private val mediaQueryService: MediaQueryService
) {
    private val logger = LoggerFactory.getLogger(VisitMediaAdapter::class.java)

    /**
     * Upload media do wizyty (kompatybilność z istniejącym API)
     */
    fun uploadMedia(visitId: String, request: MultipartHttpServletRequest): MediaUploadResponse {
        logger.debug("Adapter: uploading media to visit: {}", visitId)

        val response = mediaCommandService.uploadMediaForVisit(visitId, request)

        // Konwersja odpowiedzi do starego formatu
        return MediaUploadResponse(
            mediaId = response.id,
            protocolId = visitId
        )
    }

    /**
     * Pobieranie media wizyty (kompatybilność z istniejącym API)
     */
    fun getVisitMedia(visitId: String): List<VisitMediaResponse> {
        logger.debug("Adapter: getting media for visit: {}", visitId)

        return mediaQueryService.getVisitMedia(visitId)
            .map { media ->
                VisitMediaResponse.from(media)
            }
    }

    /**
     * Usuń media (kompatybilność z istniejącym API)
     */
    fun deleteMedia(mediaId: String) {
        logger.debug("Adapter: deleting media: {}", mediaId)
        mediaCommandService.deleteMedia(mediaId)
    }

    /**
     * Pobierz dane pliku media (kompatybilność z istniejącym API)
     */
    fun getMediaFile(mediaId: String): ByteArray? {
        logger.debug("Adapter: getting media file: {}", mediaId)
        return mediaQueryService.getMediaFile(mediaId)
    }

    /**
     * Pobierz zdjęcie z metadanymi (kompatybilność z istniejącym API)
     */
    fun getImageWithMetadata(fileId: String): GetMediaQuery? {
        logger.debug("Adapter: getting image with metadata: {}", fileId)
        return mediaQueryService.getImageWithMetadata(fileId)
    }

    /**
     * Sprawdź czy media istnieje
     */
    fun existsMedia(mediaId: String): Boolean {
        return mediaQueryService.existsMedia(mediaId)
    }
}