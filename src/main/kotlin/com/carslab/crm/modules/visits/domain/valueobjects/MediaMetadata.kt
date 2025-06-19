package com.carslab.crm.modules.visits.domain.valueobjects

data class MediaMetadata(
    val name: String,
    val contentType: String,
    val size: Long,
    val description: String? = null,
    val location: String? = null,
    val tags: Set<String> = emptySet()
) {
    init {
        require(name.isNotBlank()) { "Media name cannot be blank" }
        require(size > 0) { "Media size must be positive" }
        require(contentType.isNotBlank()) { "Content type cannot be blank" }
    }
}