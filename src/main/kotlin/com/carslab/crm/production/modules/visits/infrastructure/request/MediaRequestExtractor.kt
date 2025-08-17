package com.carslab.crm.production.modules.visits.infrastructure.request

import com.carslab.crm.production.modules.visits.application.dto.UploadMediaRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile

@Component
class MediaRequestExtractor(
    private val objectMapper: ObjectMapper,
    private val requestFormatDetector: RequestFormatDetector
) {

    fun extractMediaRequest(request: MultipartHttpServletRequest): UploadMediaRequest {
        val format = requestFormatDetector.detectFormat(request)

        return when (format) {
            RequestFormat.IMAGE_ARRAY -> extractFromImageArrayFormat(request)
            RequestFormat.STANDARD -> extractFromStandardFormat(request)
            RequestFormat.UNKNOWN -> throw IllegalArgumentException("Unknown request format")
        }
    }

    private fun extractFromImageArrayFormat(request: MultipartHttpServletRequest): UploadMediaRequest {
        val imageFile = request.fileMap.entries
            .firstOrNull { it.key.matches(Regex("images\\[\\d+\\]")) }
            ?.value
            ?: throw IllegalArgumentException("No image file found")

        val metadataJson = request.getParameter("image")
            ?: throw IllegalArgumentException("No image metadata found")

        val metadata = objectMapper.readValue(metadataJson, ImageMetadata::class.java)

        return UploadMediaRequest(
            file = imageFile,
            name = metadata.name,
            description = null,
            location = null,
            tags = metadata.tags
        )
    }

    private fun extractFromStandardFormat(request: MultipartHttpServletRequest): UploadMediaRequest {
        val file = request.getFile("file")
            ?: throw IllegalArgumentException("No file found")

        val metadataJson = request.getParameter("mediaDetails")
            ?: throw IllegalArgumentException("No media details found")

        val metadata = objectMapper.readValue(metadataJson, MediaDetailsRequest::class.java)

        return UploadMediaRequest(
            file = file,
            name = metadata.name,
            description = metadata.description,
            location = metadata.location,
            tags = metadata.tags
        )
    }
}

data class ImageMetadata(
    val name: String,
    val size: Long,
    val type: String,
    val tags: List<String>,
    val has_file: Boolean
)

data class MediaDetailsRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)