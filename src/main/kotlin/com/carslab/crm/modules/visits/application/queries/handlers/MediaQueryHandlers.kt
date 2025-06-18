// src/main/kotlin/com/carslab/crm/modules/visits/application/queries/handlers/MediaQueryHandlers.kt
package com.carslab.crm.modules.visits.application.queries.handlers

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.modules.visits.domain.services.VisitMediaService
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.domain.model.ProtocolId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GetVisitMediaQueryHandler(
    private val visitMediaService: VisitMediaService
) : QueryHandler<GetVisitMediaQuery, List<MediaReadModel>> {

    private val logger = LoggerFactory.getLogger(GetVisitMediaQuery::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun handle(query: GetVisitMediaQuery): List<MediaReadModel> {
        logger.debug("Getting media for visit: ${query.visitId}")

        val visitId = ProtocolId(query.visitId)
        val mediaItems = visitMediaService.getVisitMedia(visitId)

        return mediaItems.map { media ->
            MediaReadModel(
                id = media.id,
                name = media.name,
                size = media.size,
                contentType = "", // TODO,
                description = "", // TODO,
                location = "", // TODO,
                tags = media.tags,
                createdAt = "", // TODO,
                downloadUrl = "/api/v1/protocols/media/${media.id}/download"
            )
        }
    }
}

@Service
class GetMediaByIdQueryHandler(
    private val visitMediaService: VisitMediaService
) : QueryHandler<GetMediaByIdQuery, MediaReadModel?> {

    private val logger = LoggerFactory.getLogger(GetMediaByIdQueryHandler::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun handle(query: GetMediaByIdQuery): MediaReadModel? {
        logger.debug("Getting media by ID: ${query.mediaId}")

        val metadata = visitMediaService.getMediaMetadata(query.mediaId)
            ?: return null

        return MediaReadModel(
            id = query.mediaId,
            name = metadata.originalName,
            size = metadata.size,
            contentType = metadata.contentType,
            description = null, // TODO: Add description to metadata if needed
            location = null,    // TODO: Add location to metadata if needed
            tags = metadata.tags,
            createdAt = java.time.LocalDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(metadata.uploadTime), java.time.ZoneOffset.UTC)
                .format(dateFormatter),
            downloadUrl = "/api/v1/protocols/media/${query.mediaId}/download"
        )
    }
}

@Service
class GetMediaFileQueryHandler(
    private val visitMediaService: VisitMediaService
) : QueryHandler<GetMediaFileQuery, MediaFileReadModel?> {

    private val logger = LoggerFactory.getLogger(GetMediaFileQueryHandler::class.java)

    override fun handle(query: GetMediaFileQuery): MediaFileReadModel? {
        logger.debug("Getting media file data: ${query.mediaId}")

        val fileData = visitMediaService.getMediaData(query.mediaId)
            ?: return null

        val metadata = visitMediaService.getMediaMetadata(query.mediaId)
            ?: return null

        return MediaFileReadModel(
            data = fileData,
            contentType = metadata.contentType,
            originalName = metadata.originalName,
            size = metadata.size
        )
    }
}