package com.carslab.crm.production.modules.associations.infrastructure.mapper

import com.carslab.crm.production.modules.associations.domain.model.AssociationId
import com.carslab.crm.production.modules.associations.domain.model.ClientVehicleAssociation
import com.carslab.crm.production.modules.associations.infrastructure.entity.ClientVehicleAssociationEntity
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId

fun ClientVehicleAssociation.toEntity(): ClientVehicleAssociationEntity {
    return ClientVehicleAssociationEntity(
        id = this.id?.value,
        clientId = this.clientId.value,
        vehicleId = this.vehicleId.value,
        companyId = this.companyId,
        associationType = this.associationType,
        isPrimary = this.isPrimary,
        startDate = this.startDate,
        endDate = this.endDate,
        createdAt = this.createdAt
    )
}

fun ClientVehicleAssociationEntity.toDomain(): ClientVehicleAssociation {
    return ClientVehicleAssociation(
        id = this.id?.let { AssociationId.of(it) },
        clientId = ClientId.of(this.clientId),
        vehicleId = VehicleId.of(this.vehicleId),
        companyId = this.companyId,
        associationType = this.associationType,
        isPrimary = this.isPrimary,
        startDate = this.startDate,
        endDate = this.endDate,
        createdAt = this.createdAt
    )
}