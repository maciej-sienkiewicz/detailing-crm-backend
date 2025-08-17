package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.MediaUploadResponse
import com.carslab.crm.production.modules.visits.application.dto.UploadMediaRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.MediaMetadata
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitMediaService
import com.carslab.crm.production.modules.visits.infrastructure.request.MediaRequestExtractor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartHttpServletRequest

@Service
@Transactional
class VisitMediaCommandService(
    private val mediaService: VisitMediaService,
    private val securityContext: SecurityContext,
    private val mediaRequestExtractor: MediaRequestExtractor
) {
    private val logger = LoggerFactory.getLogger(VisitMediaCommandService::class.java)

    fun uploadMedia(visitId: String, request: MultipartHttpServletRequest):MediaUploadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to visit: {} for company: {}", visitId, companyId)

        val mediaRequest = mediaRequestExtractor.extractMediaRequest(request)

        val command = UploadMediaCommand(
            visitId = VisitId.of(visitId),
            file = mediaRequest.file,
            metadata = MediaMetadata(
                name = mediaRequest.name,
                description = mediaRequest.description,
                location = mediaRequest.location,
                tags = mediaRequest.tags
            )
        )

        val media = mediaService.uploadMedia(command, companyId)
        logger.info("Media uploaded successfully to visit: {}", visitId)

        return MediaUploadResponse(
            media.id,
            media.visitId.toString()
        )
    }
}