package com.carslab.crm.domain.model.view.protocol

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import java.time.LocalDateTime

data class ProtocolView(
    val id: ProtocolId,
    val title: String,
    val vehicleId: VehicleId,
    val clientId: ClientId,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val notes: String?,
    val createdAt: LocalDateTime,
    val keysProvided: Boolean,
    val documentsProvided: Boolean,
    val calendarColorId: CalendarColorId
)