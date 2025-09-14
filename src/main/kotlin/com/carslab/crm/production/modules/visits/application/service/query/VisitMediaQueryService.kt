package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.production.modules.media.application.adapter.VisitMediaAdapter
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitMediaQueryService(
    private val visitMediaAdapter: VisitMediaAdapter // Nowa zależność
) {
    private val logger = LoggerFactory.getLogger(VisitMediaQueryService::class.java)

    fun getVisitMedia(visitId: String): List<VisitMediaResponse> {
        logger.debug("Fetching media for visit: {}", visitId)

        // Delegacja do adaptera
        return visitMediaAdapter.getVisitMedia(visitId)
    }

    fun getMediaFile(mediaId: String): ByteArray? {
        logger.debug("Fetching media file: {}", mediaId)

        // Delegacja do adaptera
        return visitMediaAdapter.getMediaFile(mediaId)
    }

    fun getImageWithMetadata(fileId: String): GetMediaQuery? {
        logger.debug("Fetching image with metadata: {}", fileId)

        // Delegacja do adaptera
        return visitMediaAdapter.getImageWithMetadata(fileId)
    }
}