package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleStatistics
import com.carslab.crm.modules.clients.domain.port.VehicleStatisticsRepositoryDeprecated
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class VehicleStatisticsServiceDeprecated(
    private val vehicleStatisticsRepositoryDeprecated: VehicleStatisticsRepositoryDeprecated
) {
    private val logger = LoggerFactory.getLogger(VehicleStatisticsServiceDeprecated::class.java)

    fun updateLastVisitDate(vehicleId: VehicleId, visitDate: LocalDateTime = LocalDateTime.now()) {
        try {
            val stats = vehicleStatisticsRepositoryDeprecated.findByVehicleId(vehicleId)
                ?: createInitialStatistics(vehicleId, visitDate)

            val updatedStats = stats.copy(lastVisitDate = visitDate)
            vehicleStatisticsRepositoryDeprecated.save(updatedStats)

            logger.debug("Updated last visit date for vehicle: ${vehicleId.value}")
        } catch (e: Exception) {
            logger.warn("Failed to update vehicle statistics for vehicle: ${vehicleId.value}", e)
            // Graceful degradation - nie przerywamy głównego procesu
        }
    }

    private fun createInitialStatistics(vehicleId: VehicleId, visitDate: LocalDateTime): VehicleStatistics {
        return VehicleStatistics(
            vehicleId = vehicleId.value,
            visitCount = 0,
            totalRevenue = BigDecimal.ZERO,
            lastVisitDate = visitDate
        )
    }
}