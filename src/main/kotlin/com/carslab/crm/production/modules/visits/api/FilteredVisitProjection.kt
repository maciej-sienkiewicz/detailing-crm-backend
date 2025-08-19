package com.carslab.crm.production.modules.visits.api

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.time.LocalDateTime

interface FilteredVisitProjection {
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
    val calendarColorId: String
    val lastUpdate: LocalDateTime
}