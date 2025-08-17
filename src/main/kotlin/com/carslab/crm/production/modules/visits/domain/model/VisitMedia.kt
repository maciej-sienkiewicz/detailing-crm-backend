package com.carslab.crm.production.modules.visits.domain.model

import java.time.LocalDateTime

data class VisitMedia(
    val id: String,
    val visitId: Long,
    val name: String,
    val description: String?,
    val location: String?,
    val tags: List<String>,
    val type: MediaType,
    val size: Long,
    val contentType: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

enum class MediaType {
    PHOTO,
    VIDEO,
    DOCUMENT
}

data class MediaMetadata(
    val name: String,
    val description: String?,
    val location: String?,
    val tags: List<String>
) {
    init {
        require(name.isNotBlank()) { "Media name cannot be blank" }
        require(tags.size <= 20) { "Too many tags (max 20)" }
        tags.forEach { tag ->
            require(tag.length <= 50) { "Tag too long: $tag (max 50 characters)" }
        }
    }
}