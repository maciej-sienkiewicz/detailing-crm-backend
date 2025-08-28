package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import org.springframework.stereotype.Component

@Component
class VehicleStatisticsInitializer(
    private val vehicleStatisticsRepository: VehicleStatisticsRepository
) {
    fun initialize(vehicleId: VehicleId) {
        val statistics = VehicleStatistics(vehicleId = vehicleId)
        vehicleStatisticsRepository.save(statistics)
    }
}