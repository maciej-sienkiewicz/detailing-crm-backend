package com.carslab.crm.production.modules.services.infrastructure.mapper

import com.carslab.crm.production.modules.services.domain.model.Service
import com.carslab.crm.production.modules.services.domain.model.ServiceId
import com.carslab.crm.production.modules.services.infrastructure.entity.ServiceEntity

fun Service.toEntity(): ServiceEntity {
    return ServiceEntity(
        id = this.id.value,
        companyId = this.companyId,
        name = this.name,
        description = this.description,
        price = this.price,
        vatRate = this.vatRate,
        isActive = this.isActive,
        previousVersionId = this.previousVersionId?.value,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}

fun ServiceEntity.toDomain(): Service {
    return Service(
        id = ServiceId.of(this.id),
        companyId = this.companyId,
        name = this.name,
        description = this.description,
        price = this.price,
        vatRate = this.vatRate,
        isActive = this.isActive,
        previousVersionId = this.previousVersionId?.let { ServiceId.of(it) },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}