package com.carslab.crm.modules.visits.infrastructure.processor.dto

data class RawMediaMetadata(
    val name: String,
    val size: Long? = null,
    val type: String? = null,
    val description: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val hasFile: Boolean = false
)