package com.carslab.crm.domain.model

import java.time.LocalDate
import java.util.UUID

data class ServiceHistoryId(val value: String) {
    companion object {
        fun generate(): ServiceHistoryId = ServiceHistoryId(UUID.randomUUID().toString())
    }
}

data class ServiceHistory(
    val id: ServiceHistoryId,
    val vehicleId: VehicleId,
    val date: LocalDate,
    val serviceType: String,
    val description: String,
    val price: Double,
    val protocolId: String? = null,
    val audit: Audit
)