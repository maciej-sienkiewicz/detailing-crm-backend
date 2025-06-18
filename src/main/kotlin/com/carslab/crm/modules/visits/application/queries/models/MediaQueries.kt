// src/main/kotlin/com/carslab/crm/modules/visits/application/queries/models/MediaQueries.kt
package com.carslab.crm.modules.visits.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query

/**
 * Query to get all media for a visit
 */
data class GetVisitMediaQuery(
    val visitId: String
) : Query<List<MediaReadModel>>

/**
 * Query to get specific media item
 */
data class GetMediaByIdQuery(
    val mediaId: String
) : Query<MediaReadModel?>

/**
 * Query to get media file data
 */
data class GetMediaFileQuery(
    val mediaId: String
) : Query<MediaFileReadModel?>

/**
 * Media read model for API responses
 */
data class MediaReadModel(
    val id: String,
    val name: String,
    val size: Long,
    val contentType: String,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String,
    val downloadUrl: String
)

/**
 * Media file data for serving files
 */
data class MediaFileReadModel(
    val data: ByteArray,
    val contentType: String,
    val originalName: String,
    val size: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaFileReadModel

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