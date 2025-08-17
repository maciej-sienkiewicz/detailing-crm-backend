package com.carslab.crm.production.modules.visits.application.queries.models

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.math.BigDecimal
import java.time.LocalDateTime

interface VisitListProjection {
    val visitId: Long
    val title: String
    val clientName: String
    val companyName: String?
    val vehicleMake: String
    val vehicleModel: String
    val licensePlate: String
    val productionYear: Int?
    val color: String?
    val startDate: LocalDateTime
    val endDate: LocalDateTime
    val status: VisitStatus
    val totalServiceCount: Int
    val totalAmount: BigDecimal
    val calendarColorId: String
    val lastUpdate: LocalDateTime
}