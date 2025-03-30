package com.carslab.crm.domain.model.view.protocol

import com.carslab.crm.domain.model.*
import java.time.LocalDateTime

data class ProtocolView(
    val id: ProtocolId,
    val vehicleId: VehicleId,
    val clientId: ClientId,
    val period: ServicePeriod,
    val status: ProtocolStatus,
    val notes: String?,
    val createdAt: LocalDateTime,
)