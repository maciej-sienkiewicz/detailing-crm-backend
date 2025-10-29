package com.carslab.crm.production.modules.visits.application.queries.models

import java.math.BigDecimal

data class VisitListReadModel(
    val id: String,
    val title: String,
    val vehicle: VehicleBasicReadModel,
    val client: ClientBasicReadModel,
    val period: PeriodReadModel,
    val status: String,
    val calendarColorId: String,
    val totalServiceCount: Int,
    val totalAmountNetto: BigDecimal,
    val totalAmountBrutto: BigDecimal,
    val totalTaxAmount: BigDecimal,
    val services: List<VisitServiceReadModel>,
    val lastUpdate: String
)

data class VisitServiceReadModel(
    val id: String,
    val name: String,
    val finalPriceNetto: BigDecimal,
    val finalPriceBrutto: BigDecimal,
    val finalTaxAmount: BigDecimal
)

data class VehicleBasicReadModel(
    val make: String,
    val model: String,
    val licensePlate: String,
    val productionYear: Int,
    val color: String?
)

data class ClientBasicReadModel(
    val name: String,
    val companyName: String?
)

data class PeriodReadModel(
    val startDate: String,
    val endDate: String
)