package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.VehicleId
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.port.VehicleStatisticsRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryVehicleStatisticsRepository: VehicleStatisticsRepository {
    private val stats = ConcurrentHashMap<Long, VehicleStats>()

    override fun save(vehicleStats: VehicleStats): VehicleStats {
        stats[vehicleStats.vehicleId] = vehicleStats
        return vehicleStats
    }

    override fun findById(id: VehicleId): VehicleStats {
        return stats[id.value.toLong()] ?: VehicleStats(id.value, 0, "0".toBigDecimal())
    }
}