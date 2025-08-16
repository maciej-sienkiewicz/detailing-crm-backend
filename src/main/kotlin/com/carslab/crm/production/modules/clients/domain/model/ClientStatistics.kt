package com.carslab.crm.production.modules.clients.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class ClientStatistics(
    val clientId: ClientId,
    val visitCount: Long = 0,
    val totalRevenue: BigDecimal = BigDecimal.ZERO,
    val vehicleCount: Long = 0,
    val lastVisitDate: LocalDateTime? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun incrementVisitCount(): ClientStatistics {
        return copy(
            visitCount = visitCount + 1,
            updatedAt = LocalDateTime.now()
        )
    }

    fun addRevenue(amount: BigDecimal): ClientStatistics {
        return copy(
            totalRevenue = totalRevenue + amount,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateVehicleCount(newCount: Long): ClientStatistics {
        return copy(
            vehicleCount = newCount,
            updatedAt = LocalDateTime.now()
        )
    }

    fun updateLastVisitDate(visitDate: LocalDateTime): ClientStatistics {
        return copy(
            lastVisitDate = visitDate,
            updatedAt = LocalDateTime.now()
        )
    }
}