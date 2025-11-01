package com.carslab.crm.production.modules.clients.infrastructure.mapper

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientEntity
import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientStatisticsEntity
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject

fun Client.toEntity(): ClientEntity {
    return ClientEntity(
        id = if (this.id.value == 0L) null else this.id.value,
        companyId = this.companyId,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        phone = this.phone,
        address = this.address,
        company = this.company,
        taxId = this.taxId,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun ClientEntity.toDomain(): Client {
    return Client(
        id = ClientId.of(this.id!!),
        companyId = this.companyId,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        phone = this.phone,
        address = this.address,
        company = this.company,
        taxId = this.taxId,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun ClientStatistics.toEntity(): ClientStatisticsEntity {
    return ClientStatisticsEntity(
        clientId = this.clientId.value,
        visitCount = this.visitCount,
        totalRevenueNetto = this.totalRevenue.priceNetto,
        totalRevenueBrutto = this.totalRevenue.priceBrutto,
        totalTaxAmount = this.totalRevenue.taxAmount,
        vehicleCount = this.vehicleCount,
        lastVisitDate = this.lastVisitDate,
        updatedAt = this.updatedAt
    )
}

fun ClientStatisticsEntity.toDomain(): ClientStatistics {
    return ClientStatistics(
        clientId = ClientId.of(this.clientId),
        visitCount = this.visitCount,
        totalRevenue = PriceValueObject(
            priceNetto = this.totalRevenueNetto,
            priceBrutto = this.totalRevenueBrutto,
            taxAmount = this.totalTaxAmount,
        ),
        vehicleCount = this.vehicleCount,
        lastVisitDate = this.lastVisitDate,
        updatedAt = this.updatedAt
    )
}