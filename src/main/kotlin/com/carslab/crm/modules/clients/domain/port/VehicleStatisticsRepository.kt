package com.carslab.crm.clients.domain.port

import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.clients.domain.model.VehicleStatistics
import java.math.BigDecimal

interface VehicleStatisticsRepository {
    fun findByVehicleId(vehicleId: VehicleId): VehicleStatistics?
    fun save(statistics: VehicleStatistics): VehicleStatistics
    fun updateVisitCount(vehicleId: VehicleId, increment: Long)
    fun updateRevenue(vehicleId: VehicleId, amount: BigDecimal)
    fun recalculateStatistics(vehicleId: VehicleId): VehicleStatistics
    fun deleteByVehicleId(vehicleId: VehicleId): Boolean
}