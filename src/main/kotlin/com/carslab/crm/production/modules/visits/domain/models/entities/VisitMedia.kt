package com.carslab.crm.production.modules.visits.domain.models.entities

import com.carslab.crm.production.modules.visits.domain.models.enums.MediaType
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