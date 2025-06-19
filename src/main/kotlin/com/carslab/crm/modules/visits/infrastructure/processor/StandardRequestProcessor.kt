package com.carslab.crm.modules.visits.infrastructure.processor

import com.carslab.crm.modules.visits.domain.valueobjects.MediaMetadata
import com.carslab.crm.modules.visits.infrastructure.processor.dto.MediaUploadData
import com.carslab.crm.modules.visits.infrastructure.processor.dto.RawMediaMetadata
import com.carslab.crm.modules.visits.infrastructure.processor.exceptions.InvalidRequestException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest

@Component
class StandardRequestProcessor(
    private val objectMapper: ObjectMapper
) : MultipartRequestProcessor {

    override fun canProcess(request: MultipartHttpServletRequest): Boolean {
        return request.getFile("file") != null &&
                request.getParameter("mediaDetails") != null
    }

    override fun extractMediaData(request: MultipartHttpServletRequest): MediaUploadData {
        val file = request.getFile("file")
            ?: throw InvalidRequestException("No file found")

        val metadataJson = request.getParameter("mediaDetails")
            ?: throw InvalidRequestException("No media details found")

        // Parse JSON metadata
        val rawMetadata = objectMapper.readValue(metadataJson, RawMediaMetadata::class.java)

        // Create domain metadata
        val metadata = MediaMetadata(
            name = rawMetadata.name,
            contentType = file.contentType ?: "application/octet-stream",
            size = file.size,
            description = rawMetadata.description,
            location = rawMetadata.location,
            tags = rawMetadata.tags.toSet()
        )

        return MediaUploadData(
            fileData = file,
            metadata = metadata
        )
    }
}