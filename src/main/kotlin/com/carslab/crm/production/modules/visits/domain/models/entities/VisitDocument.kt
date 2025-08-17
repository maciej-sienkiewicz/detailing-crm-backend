package com.carslab.crm.production.modules.visits.domain.models.entities

import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.models.enums.DocumentType
import java.time.LocalDateTime

data class VisitDocument(
    val id: String,
    val visitId: VisitId,
    val name: String,
    val type: DocumentType,
    val size: Long,
    val contentType: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val uploadedBy: String
)