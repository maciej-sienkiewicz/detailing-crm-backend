package com.carslab.crm.production.modules.visits.infrastructure.persistence.projections

import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.math.BigDecimal
import java.time.LocalDateTime

interface VisitSearchProjection {
    val visitId: Long
    val title: String
    val clientName: String
    val vehicleMake: String
    val vehicleModel: String
    val licensePlate: String
    val status: VisitStatus
    val totalAmount: BigDecimal
    val startDate: LocalDateTime
    val endDate: LocalDateTime
    val serviceName: String?
}