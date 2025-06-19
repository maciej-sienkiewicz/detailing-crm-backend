package com.carslab.crm.modules.visits.infrastructure.processor

import com.carslab.crm.modules.visits.domain.valueobjects.MediaMetadata
import com.carslab.crm.modules.visits.infrastructure.processor.dto.MediaUploadData
import com.carslab.crm.modules.visits.infrastructure.processor.dto.RawMediaMetadata
import com.carslab.crm.modules.visits.infrastructure.processor.exceptions.InvalidRequestException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest

@Component
class ImageArrayRequestProcessor(
    private val objectMapper: ObjectMapper
) : MultipartRequestProcessor {

    override fun canProcess(request: MultipartHttpServletRequest): Boolean {
        return request.fileMap.keys.any { it.matches(Regex("images\\[\\d+\\]")) } &&
                request.getParameter("image") != null
    }

    override fun extractMediaData(request: MultipartHttpServletRequest): MediaUploadData {
        val imageFile = request.fileMap.entries
            .firstOrNull { it.key.matches(Regex("images\\[\\d+\\]")) }
            ?.value
            ?: throw InvalidRequestException("No image file found")

        val metadataJson = request.getParameter("image")
            ?: throw InvalidRequestException("No image metadata found")

        val rawMetadata = objectMapper.readValue(metadataJson, RawMediaMetadata::class.java)

        return MediaUploadData(
            fileData = imageFile,
            metadata = MediaMetadata(
                name = rawMetadata.name,
                contentType = imageFile.contentType ?: "application/octet-stream",
                size = imageFile.size,
                description = rawMetadata.description,
                location = rawMetadata.location,
                tags = rawMetadata.tags.toSet()
            )
        )
    }
}