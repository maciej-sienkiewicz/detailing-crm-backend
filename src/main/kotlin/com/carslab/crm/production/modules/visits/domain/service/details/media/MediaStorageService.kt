package com.carslab.crm.production.modules.visits.domain.service.details.media

import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.storage.UniversalStoreRequest
import com.carslab.crm.production.modules.visits.domain.command.UploadMediaCommand
import org.springframework.stereotype.Service

@Service
class MediaStorageService(
    private val storageService: UniversalStorageService
) {

    fun storeMediaFile(command: UploadMediaCommand, companyId: Long): String {
        return storageService.storeFile(
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
                tags = buildTagsMap(command.metadata.tags)
            )
        )
    }

    fun retrieveMediaData(mediaId: String): ByteArray? {
        return try {
            storageService.retrieveFile(mediaId)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteMediaFile(mediaId: String) {
        try {
            storageService.deleteFile(mediaId)
        } catch (e: Exception) {
            // Log but don't fail if storage deletion fails
        }
    }

    private fun buildTagsMap(tags: List<String>): Map<String, String> {
        return tags.mapIndexed { index, tag -> "tag_$index" to tag }.toMap()
    }
}