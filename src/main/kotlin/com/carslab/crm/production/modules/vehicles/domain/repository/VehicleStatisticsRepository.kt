package com.carslab.crm.production.modules.vehicles.domain.repository

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import java.math.BigDecimal

interface VehicleStatisticsRepository {
    fun findByVehicleId(vehicleId: VehicleId): VehicleStatistics?
    fun findByVehicleIds(vehicleIds: List<VehicleId>): List<VehicleStatistics>
    fun save(statistics: VehicleStatistics): VehicleStatistics
    fun incrementVisitCount(vehicleId: VehicleId)
    fun addRevenue(vehicleId: VehicleId, amount: BigDecimal)
    fun deleteByVehicleId(vehicleId: VehicleId): Boolean
}