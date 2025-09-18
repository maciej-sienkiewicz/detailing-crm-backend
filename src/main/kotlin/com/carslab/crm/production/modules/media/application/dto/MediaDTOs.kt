package com.carslab.crm.production.modules.media.application.dto

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaContext
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

/**
 * Response dla media
 */
data class MediaResponse(
    val id: String,
    val context: String,
    @JsonProperty("entity_id")
    val entityId: Long?,
    @JsonProperty("visit_id")
    val visitId: String?,
    @JsonProperty("vehicle_id")
    val vehicleId: String?,
    val name: String,
    val description: String?,
    val location: String?,
    val tags: List<String>,
    val type: String,
    val size: Long,
    @JsonProperty("content_type")
    val contentType: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("download_url")
    val downloadUrl: String,
    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String?
) {
    companion object {
        fun from(media: Media): MediaResponse {
            return MediaResponse(
                id = media.id.value,
                context = media.context.name,
                entityId = media.entityId,
                visitId = media.visitId?.toString(),
                vehicleId = media.vehicleId?.toString(),
                name = media.name,
                description = media.description,
                location = media.location,
                tags = media.tags,
                type = media.type.name,
                size = media.size,
                contentType = media.contentType,
                createdAt = media.createdAt,
                downloadUrl = "/api/media/${media.id.value}/download",
                thumbnailUrl = "/api/media/${media.id.value}/thumbnail"
            )
        }
    }
}

/**
 * Response dla upload media
 */
data class MediaUploadResponse(
    val id: String,
    @JsonProperty("entity_id")
    val entityId: String?,
    val message: String = "Media uploaded successfully"
)

/**
 * Request do upload media
 */
data class UploadMediaRequest(
    val file: MultipartFile,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * Response dla pobierania pliku media z metadanymi
 */
data class MediaFileResponse(
    val data: ByteArray,
    val contentType: String,
    val originalName: String,
    val size: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaFileResponse

        if (!data.contentEquals(other.data)) return false
        if (contentType != other.contentType) return false
        if (originalName != other.originalName) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}

/**
 * Response dla pobrania media z galerii
 */
data class GalleryMediaResponse(
    val id: String,
    val name: String,
    @JsonProperty("vehicle_info")
    val vehicleInfo: String?,
    @JsonProperty("visit_info")
    val visitInfo: String?,
    val size: Long,
    @JsonProperty("content_type")
    val contentType: String,
    val description: String?,
    val location: String?,
    val tags: List<String>,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    @JsonProperty("thumbnail_url")
    val thumbnailUrl: String,
    @JsonProperty("download_url")
    val downloadUrl: String
) {
    companion object {
        fun from(media: Media, vehicleInfo: String? = null, visitInfo: String? = null): GalleryMediaResponse {
            return GalleryMediaResponse(
                id = media.id.value,
                name = media.name,
                vehicleInfo = vehicleInfo,
                visitInfo = visitInfo,
                size = media.size,
                contentType = media.contentType,
                description = media.description,
                location = media.location,
                tags = media.tags,
                createdAt = media.createdAt,
                thumbnailUrl = "/api/media/${media.id.value}/thumbnail",
                downloadUrl = "/api/media/${media.id.value}/download"
            )
        }
    }
}

/**
 * Statystyki media
 */
data class MediaStatsResponse(
    @JsonProperty("total_count")
    val totalCount: Long,
    @JsonProperty("total_size")
    val totalSize: Long,
    @JsonProperty("vehicle_media_count")
    val vehicleMediaCount: Long,
    @JsonProperty("visit_media_count")
    val visitMediaCount: Long,
    @JsonProperty("by_type")
    val byType: Map<String, Long>
)

data class UpdateMediaTagsRequest(
    val tags: List<String>
) {
    init {
        require(tags.size <= 20) { "Too many tags (max 20)" }
        require(tags.all { it.isNotBlank() && it.length <= 50 }) { "Tags must be non-blank and max 50 characters" }
    }
}