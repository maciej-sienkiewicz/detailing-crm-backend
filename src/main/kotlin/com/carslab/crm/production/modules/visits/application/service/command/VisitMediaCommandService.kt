package com.carslab.crm.production.modules.visits.application.service.command

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.UploadMediaRequest
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import com.carslab.crm.production.modules.visits.domain.models.value_objects.MediaMetadata
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.service.VisitMediaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VisitMediaCommandService(
    private val mediaService: VisitMediaService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitMediaCommandService::class.java)

    fun uploadMedia(visitId: String, request: UploadMediaRequest): VisitMediaResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading media to visit: {} for company: {}", visitId, companyId)

        val command = UploadMediaCommand(
            visitId = VisitId.of(visitId),
            file = request.file,
            metadata = MediaMetadata(
                name = request.name,
                description = request.description,
                location = request.location,
                tags = request.tags
            )
        )

        val media = mediaService.uploadMedia(command, companyId)
        logger.info("Media uploaded successfully to visit: {}", visitId)

        return VisitMediaResponse.from(media)
    }
}