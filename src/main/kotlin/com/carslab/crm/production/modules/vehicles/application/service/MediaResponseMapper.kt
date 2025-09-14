package com.carslab.crm.production.modules.media.application.mapper

import com.carslab.crm.production.modules.media.application.dto.MediaResponse
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType

/**
 * Mapper do konwersji między MediaResponse a VisitMediaResponse
 * dla zachowania kompatybilności wstecznej
 */
object MediaResponseMapper {

    /**
     * Konwertuje MediaResponse na VisitMediaResponse
     */
    fun toVisitMediaResponse(media: MediaResponse): VisitMediaResponse {
        return VisitMediaResponse(
            id = media.id,
            name = media.name,
            description = media.description,
            location = media.location,
            tags = media.tags,
            type = try {
                MediaType.valueOf(media.type)
            } catch (e: IllegalArgumentException) {
                MediaType.PHOTO // Default fallback
            },
            size = media.size,
            contentType = media.contentType,
            createdAt = media.createdAt,
            updatedAt = media.createdAt, // Używamy createdAt jako updatedAt
            downloadUrl = media.downloadUrl
        )
    }

    /**
     * Konwertuje listę MediaResponse na listę VisitMediaResponse
     */
    fun toVisitMediaResponseList(mediaList: List<MediaResponse>): List<VisitMediaResponse> {
        return mediaList.map { toVisitMediaResponse(it) }
    }
}