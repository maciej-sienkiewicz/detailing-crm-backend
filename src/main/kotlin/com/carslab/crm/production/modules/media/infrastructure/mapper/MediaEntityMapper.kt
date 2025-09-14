package com.carslab.crm.production.modules.media.infrastructure.mapper

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.infrastructure.entity.MediaEntity
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

fun Media.toEntity(): MediaEntity {
    return MediaEntity(
        id = this.id.value,
        companyId = this.companyId,
        context = this.context,
        entityId = this.entityId,
        visitId = this.visitId?.value,
        vehicleId = this.vehicleId?.value,
        name = this.name,
        description = this.description,
        location = this.location,
        tags = this.tags.joinToString(","),
        type = this.type,
        size = this.size,
        contentType = this.contentType,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun MediaEntity.toDomain(): Media {
    return Media(
        id = MediaId.of(this.id),
        companyId = this.companyId,
        context = this.context,
        entityId = this.entityId,
        visitId = this.visitId?.let { VisitId.of(it) },
        vehicleId = this.vehicleId?.let { VehicleId.of(it) },
        name = this.name,
        description = this.description,
        location = this.location,
        tags = if (this.tags.isBlank()) emptyList() else this.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        type = this.type,
        size = this.size,
        contentType = this.contentType,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}