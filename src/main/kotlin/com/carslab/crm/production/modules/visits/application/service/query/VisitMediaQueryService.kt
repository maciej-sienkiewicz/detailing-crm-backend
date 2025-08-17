package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitMediaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitMediaQueryService(
    private val mediaService: VisitMediaService
) {
    private val logger = LoggerFactory.getLogger(VisitMediaQueryService::class.java)

    fun getVisitMedia(visitId: String): List<VisitMediaResponse> {
        logger.debug("Fetching media for visit: {}", visitId)

        val media = mediaService.getMediaForVisit(VisitId.of(visitId))
        return media.map { VisitMediaResponse.from(it) }
    }

    fun getMediaFile(mediaId: String): ByteArray? {
        logger.debug("Fetching media file: {}", mediaId)
        return mediaService.getMediaData(mediaId)
    }

    fun getImageWithMetadata(fileId: String): GetMediaQuery? {
        logger.debug("Fetching image with metadata: {}", fileId)
        return mediaService.getImageWithMetadata(fileId)
    }
}