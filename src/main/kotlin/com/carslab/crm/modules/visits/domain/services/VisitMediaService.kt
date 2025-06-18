package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import com.carslab.crm.domain.model.MediaType
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService.ImageMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class VisitMediaService(
    private val imageStorageService: FileImageStorageService
) {
    private val logger = LoggerFactory.getLogger(VisitMediaService::class.java)
    
    fun uploadMedia(
        visitId: ProtocolId,
        file: MultipartFile,
        name: String,
        description: String? = null,
        location: String? = null,
        tags: List<String> = emptyList()
    ): String {
        logger.info("Uploading media '{}' for visit {}", name, visitId.value)

        val mediaModel = CreateMediaTypeModel(
            type = MediaType.PHOTO,
            name = name,
            description = description,
            location = location,
            tags = tags
        )

        return imageStorageService.storeFile(file, visitId, mediaModel)
    }

    /**
     * Update media metadata
     */
    fun updateMediaMetadata(
        visitId: ProtocolId,
        mediaId: String,
        name: String,
        description: String? = null,
        location: String? = null,
        tags: List<String> = emptyList()
    ): MediaTypeView {
        logger.info("Updating metadata for media {} in visit {}", mediaId, visitId.value)

        return imageStorageService.updateImageMetadata(
            protocolId = visitId,
            imageId = mediaId,
            name = name,
            tags = tags,
            description = description,
            location = location
        )
    }
    
    fun deleteMedia(visitId: ProtocolId, mediaId: String): Boolean {
        logger.info("Deleting media {} from visit {}", mediaId, visitId.value)

        return try {
            imageStorageService.deleteFile(mediaId, visitId)
            true
        } catch (e: Exception) {
            logger.error("Failed to delete media {} from visit {}", mediaId, visitId.value, e)
            false
        }
    }

    fun getVisitMedia(visitId: ProtocolId): Set<MediaTypeView> {
        logger.debug("Getting media for visit {}", visitId.value)
        return imageStorageService.getImagesByProtocol(visitId)
    }

    /**
     * Get media file data
     */
    fun getMediaData(mediaId: String): ByteArray? {
        logger.debug("Getting file data for media {}", mediaId)
        return imageStorageService.getFileData(mediaId)
    }

    /**
     * Get media metadata
     */
    fun getMediaMetadata(mediaId: String): ImageMetadata? {
        logger.debug("Getting metadata for media {}", mediaId)
        return imageStorageService.getFileMetadata(mediaId)
    }
}