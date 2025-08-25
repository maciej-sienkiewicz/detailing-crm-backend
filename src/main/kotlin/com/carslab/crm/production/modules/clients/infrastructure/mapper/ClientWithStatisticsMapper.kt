package com.carslab.crm.production.modules.clients.infrastructure.mapper

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.domain.model.ClientWithStatistics
import com.carslab.crm.production.modules.clients.infrastructure.dto.ClientWithStatisticsRaw
import java.math.BigDecimal


fun ClientWithStatisticsRaw.toDomain(): ClientWithStatistics {
    val client = Client(
        id = ClientId.of(getClientId()),
        companyId = getClientCompanyId(),
        firstName = getClientFirstName(),
        lastName = getClientLastName(),
        email = getClientEmail(),
        phone = getClientPhone(),
        address = getClientAddress(),
        company = getClientCompany(),
        taxId = getClientTaxId(),
        notes = getClientNotes(),
        createdAt = getClientCreatedAt(),
        updatedAt = getClientUpdatedAt(),
    )

    val statistics = if (getStatsClientId() != null) {
        ClientStatistics(
            clientId = ClientId.of(getStatsClientId()!!),
            visitCount = getStatsVisitCount() ?: 0,
            totalRevenue = getStatsTotalRevenue() ?: BigDecimal.ZERO,
            vehicleCount = getStatsVehicleCount() ?: 0,
            lastVisitDate = getStatsLastVisitDate(),
            updatedAt = getStatsUpdatedAt() ?: getClientUpdatedAt()
        )
    } else {
        null
    }

    val vehicleIds = getVehicleIds()?.let { vehicleIdsString ->
        if (vehicleIdsString.isBlank()) {
            emptyList()
        } else {
            vehicleIdsString.split(",").mapNotNull { it.toLongOrNull() }
        }
    } ?: emptyList()

    return ClientWithStatistics(
        client = client,
        statistics = statistics,
        vehicleIds = vehicleIds
    )
}