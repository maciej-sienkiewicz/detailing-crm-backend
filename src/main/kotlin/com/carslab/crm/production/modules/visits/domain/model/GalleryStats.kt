package com.carslab.crm.production.modules.visits.domain.model

data class GalleryStats(
    val totalImages: Long,
    val totalSize: Long
)

data class TagStat(
    val tag: String,
    val count: Long
)