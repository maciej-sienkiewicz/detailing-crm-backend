package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.adapter.VisitMediaAdapter
import com.carslab.crm.production.modules.visits.application.dto.MediaUploadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartHttpServletRequest

@Service
@Transactional
class VisitMediaCommandService(
    private val visitMediaAdapter: VisitMediaAdapter, // Nowa zależność
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitMediaCommandService::class.java)

    fun uploadMedia(visitId: String, request: MultipartHttpServletRequest): MediaUploadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to visit: {} for company: {}", visitId, companyId)

        // Delegacja do adaptera
        val response = visitMediaAdapter.uploadMedia(visitId, request)
        logger.info("Media uploaded successfully to visit: {}", visitId)

        return response
    }

    fun deleteMedia(mediaId: String) {
        logger.info("Deleting media with ID: {}", mediaId)

        // Delegacja do adaptera
        visitMediaAdapter.deleteMedia(mediaId)

        logger.info("Media deleted successfully: {}", mediaId)
    }
}