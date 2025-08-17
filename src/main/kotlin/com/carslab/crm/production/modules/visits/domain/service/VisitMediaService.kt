package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.production.modules.visits.application.queries.models.GetMediaQuery
import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitMedia
import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.repositories.VisitMediaRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VisitMediaService(
    private val mediaRepository: VisitMediaRepository,
    private val storageService: UniversalStorageService
) {
    fun uploadMedia(command: UploadMediaCommand, companyId: Long): VisitMedia {
        if (!mediaRepository.existsVisitByIdAndCompanyId(command.visitId, companyId)) {
            throw EntityNotFoundException("Visit not found: ${command.visitId.value}")
        }

        validateFile(command.file)
        
        val storageId = storageService.storeFile(
            UniversalStoreRequest(
                file = command.file,
                originalFileName = command.file.originalFilename ?: "image.jpg",
                contentType = command.file.contentType ?: "image/jpeg",
                companyId = companyId,
                entityId = command.visitId.value.toString(),
                entityType = "visit",
                category = "visits",
                subCategory = "media",
                description = command.metadata.description,
                tags = command.metadata.tags.mapIndexed { index, tag -> "tag_$index" to tag }.toMap()
            )
        )

        val media = VisitMedia(
            id = storageId,
            visitId = command.visitId.value,
            name = command.metadata.name,
            description = command.metadata.description,
            location = command.metadata.location,
            tags = command.metadata.tags,
            type = MediaType.PHOTO,
            size = command.file.size,
            contentType = command.file.contentType ?: "application/octet-stream",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return mediaRepository.save(media)
    }

    fun getMediaForVisit(visitId: VisitId): List<VisitMedia> {
        return mediaRepository.findByVisitId(visitId)
    }
    
    fun getMediaData(mediaId: String): ByteArray? {
        return mediaRepository.getFileData(mediaId)
    }

    fun getImageWithMetadata(fileId: String): GetMediaQuery? {
        val media = mediaRepository.findById(fileId) ?: return null
        val data = mediaRepository.getFileData(fileId) ?: return null

        return GetMediaQuery(
            data = data,
            contentType = media.contentType,
            originalName = media.name,
            size = media.size
        )
    }

    fun deleteMedia(mediaId: String): Boolean {
        return mediaRepository.deleteById(mediaId)
    }

    private fun validateFile(file: org.springframework.web.multipart.MultipartFile) {
        if (file.isEmpty) {
            throw BusinessException("File cannot be empty")
        }

        val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        if (file.contentType !in allowedTypes) {
            throw BusinessException("Only image files are allowed (JPEG, PNG, GIF, WebP)")
        }

        if (file.size > 10 * 1024 * 1024) {
            throw BusinessException("File size cannot exceed 10MB")
        }
    }
}