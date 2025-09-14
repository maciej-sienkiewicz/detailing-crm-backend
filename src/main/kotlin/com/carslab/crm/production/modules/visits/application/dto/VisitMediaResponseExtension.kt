package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.production.modules.media.application.dto.MediaResponse
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType

/**
 * Rozszerzenia dla VisitMediaResponse do kompatybilności z nowym systemem Media
 */
fun VisitMediaResponse.Companion.from(media: MediaResponse): VisitMediaResponse {
    return VisitMediaResponse(
        id = media.id,
        name = media.name,
        description = media.description,
        location = media.location,
        tags = media.tags,
        type = try {
            MediaType.valueOf(media.type)
        } catch (e: IllegalArgumentException) {
            MediaType.PHOTO // Default fallback jeśli nie można skonwertować
        },
        size = media.size,
        contentType = media.contentType,
        downloadUrl = media.downloadUrl,
        createdAt = media.createdAt,
        updatedAt = media.createdAt // Używamy createdAt jako updatedAt
    )
}

/**
 * Konwersja listy MediaResponse na listę VisitMediaResponse
 */
fun VisitMediaResponse.Companion.fromList(mediaList: List<MediaResponse>): List<VisitMediaResponse> {
    return mediaList.map { from(it) }
}