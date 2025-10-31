package com.carslab.crm.production.modules.vehicles.domain.model

import com.carslab.crm.production.shared.domain.value_objects.PriceType
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.math.BigDecimal
import java.time.LocalDateTime

data class VehicleStatistics(
    val vehicleId: VehicleId,
    val visitCount: Long = 0,
    val totalRevenue: PriceValueObject = PriceValueObject(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
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
            totalRevenue = totalRevenue.add(PriceValueObject.createFromInput(amount, PriceType.BRUTTO, 23)),
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