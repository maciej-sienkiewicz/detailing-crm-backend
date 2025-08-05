package com.carslab.crm.modules.visits.application.queries.models

import com.carslab.crm.infrastructure.cqrs.Query
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.api.model.response.PaginatedResponse
import java.math.BigDecimal
import java.time.LocalDateTime

data class SearchVisitsQuery(
    val clientName: String? = null,
    val licensePlate: String? = null,
    val status: ProtocolStatus? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val make: String? = null,
    val model: String? = null,
    val serviceName: String? = null,
    val serviceIds: List<String>? = null,
    val title: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val page: Int = 0,
    val size: Int = 10
) : Query<PaginatedResponse<VisitListReadModel>>

data class VisitListReadModel(
    val id: String,
    val title: String,
    val vehicle: VehicleBasicReadModel,
    val client: ClientBasicReadModel,
    val period: PeriodReadModel,
    val status: String,
    val calendarColorId: String,
    val totalServiceCount: Int,
    val totalAmount: BigDecimal,
    val services: List<VisitServiceReadModel>,
    val lastUpdate: String
)

data class VisitServiceReadModel(
    val id: String,
    val name: String,
    val finalPrice: BigDecimal
)