package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.VehicleStats

interface VehicleStatisticsRepository {
    fun save(vehicleStats: VehicleStats): VehicleStats
    fun findById(id: VehicleId): VehicleStats
}