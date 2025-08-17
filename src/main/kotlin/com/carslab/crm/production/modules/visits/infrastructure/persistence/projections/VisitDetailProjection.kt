package com.carslab.crm.production.modules.visits.infrastructure.persistence.projections

import java.time.LocalDateTime

data class VisitDetailProjection(
    val visitId: Long,
    val title: String,
    val calendarColorId: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val status: String,
    val notes: String?,
    val referralSource: String?,
    val appointmentId: String?,
    val keysProvided: Boolean,
    val documentsProvided: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val clientId: Long,
    val clientName: String,
    val clientEmail: String?,
    val clientPhone: String?,
    val clientAddress: String?,
    val clientCompany: String?,
    val clientTaxId: String?,
    val vehicleId: Long,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleLicensePlate: String,
    val vehicleYear: Int?,
    val vehicleMileage: Long?,
    val vehicleVin: String?,
    val vehicleColor: String?
)