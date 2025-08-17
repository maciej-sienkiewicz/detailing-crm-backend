package com.carslab.crm.production.modules.visits.domain.models.aggregates

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import java.math.BigDecimal
import java.time.LocalDateTime

data class VisitListItem(
    val visitId: VisitId,
    val title: String,
    val clientName: String,
    val companyName: String?,
    val vehicleMake: String,
    val vehicleModel: String,
    val licensePlate: String,
    val productionYear: Int?,
    val color: String?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val status: VisitStatus,
    val totalServiceCount: Int,
    val totalAmount: BigDecimal,
    val calendarColorId: String,
    val lastUpdate: LocalDateTime
)

data class VisitListService(
    val id: String,
    val name: String,
    val finalPrice: BigDecimal
)