package com.carslab.crm.production.modules.visits.domain.service.details

import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitMediaRepository
import com.carslab.crm.production.modules.visits.domain.service.details.media.MediaFileValidator
import com.carslab.crm.production.modules.visits.domain.service.details.media.MediaStorageService
import com.carslab.crm.production.modules.visits.domain.service.details.media.VisitMediaFactory
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service

@Service
class VisitMediaService(
    private val mediaRepository: VisitMediaRepository,
    private val mediaStorageService: MediaStorageService,
    private val mediaFileValidator: MediaFileValidator,
    private val mediaFactory: VisitMediaFactory
) {

    fun uploadMedia(command: UploadMediaCommand, companyId: Long): VisitMedia {
        validateVisitExists(command.visitId, companyId)
        mediaFileValidator.validateFile(command.file)

        val storageId = mediaStorageService.storeMediaFile(command, companyId)
        val media = mediaFactory.createMedia(command, storageId)

        return mediaRepository.save(media)
    }

    fun getMediaForVisit(visitId: VisitId): List<VisitMedia> {
        return mediaRepository.findByVisitId(visitId)
    }

    fun getMediaData(mediaId: String): ByteArray? {
        return mediaStorageService.retrieveMediaData(mediaId)
    }

    fun getImageWithMetadata(fileId: String): GetMediaQuery? {
        val media = mediaRepository.findById(fileId) ?: return null
        val data = mediaStorageService.retrieveMediaData(fileId) ?: return null

        return GetMediaQuery(
            data = data,
            contentType = media.contentType,
            originalName = media.name,
            size = media.size
        )
    }

    fun deleteMedia(mediaId: String): Boolean {
        val media = mediaRepository.findById(mediaId) ?: return false

        mediaStorageService.deleteMediaFile(mediaId)
        return mediaRepository.deleteById(mediaId)
    }

    private fun validateVisitExists(visitId: VisitId, companyId: Long) {
        if (!mediaRepository.existsVisitByIdAndCompanyId(visitId, companyId)) {
            throw EntityNotFoundException("Visit not found: ${visitId.value}")
        }
    }
}