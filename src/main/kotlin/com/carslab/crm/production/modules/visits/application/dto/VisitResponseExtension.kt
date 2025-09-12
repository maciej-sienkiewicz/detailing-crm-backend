package com.carslab.crm.production.modules.visits.application.dto

import com.carslab.crm.production.modules.events.application.dto.RecurringEventResponse
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import java.time.LocalDateTime

fun VisitResponse.Companion.fromRecurringEvent(
    recurringEvent: RecurringEventResponse,
    originalRequest: CreateVisitRequest
): VisitResponse {
    return VisitResponse(
        id = recurringEvent.id,
        title = recurringEvent.title,
        clientId = originalRequest.ownerId?.toString() ?: "0",
        vehicleId = "0",
        startDate = LocalDateTime.parse(originalRequest.startDate),
        endDate = LocalDateTime.parse(originalRequest.endDate ?: originalRequest.startDate),
        status = VisitStatus.SCHEDULED,
        services = emptyList(),
        totalAmount = java.math.BigDecimal.ZERO,
        serviceCount = 0,
        notes = "This is a recurring visit pattern. Individual visits will be created from this template.",
        referralSource = null,
        appointmentId = originalRequest.appointmentId,
        calendarColorId = originalRequest.calendarColorId,
        keysProvided = originalRequest.keysProvided ?: false,
        documentsProvided = originalRequest.documentsProvided ?: false,
        createdAt = recurringEvent.createdAt,
        updatedAt = recurringEvent.updatedAt
    )
}