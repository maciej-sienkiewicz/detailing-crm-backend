package com.carslab.crm.production.modules.visits.domain.models.value_objects

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