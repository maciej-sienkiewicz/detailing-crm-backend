package com.carslab.crm.production.modules.vehicles.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class VehicleStatistics(
    val vehicleId: VehicleId,
    val visitCount: Long = 0,
    val totalRevenue: BigDecimal = BigDecimal.ZERO,
    val lastVisitDate: LocalDateTime? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun incrementVisitCount(): VehicleStatistics {
        return copy(
            visitCount = visitCount + 1,
            updatedAt = LocalDateTime.now()
        )
    }

    fun addRevenue(amount: BigDecimal): VehicleStatistics {
        return copy(
            totalRevenue = totalRevenue + amount,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateLastVisitDate(visitDate: LocalDateTime): VehicleStatistics {
        return copy(
            lastVisitDate = visitDate,
            updatedAt = LocalDateTime.now()
        )
    }
}