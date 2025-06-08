package com.carslab.crm.modules.clients.domain

import com.carslab.crm.modules.clients.api.responses.VehicleCompanyStatisticsResponse
import com.carslab.crm.modules.clients.api.responses.MostActiveVehicleInfo
import com.carslab.crm.modules.clients.domain.port.VehicleCompanyStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Domain service for handling company-wide vehicle statistics
 */
@Service
@Transactional(readOnly = true)
class VehicleCompanyStatisticsService(
    private val vehicleCompanyStatisticsRepository: VehicleCompanyStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleCompanyStatisticsService::class.java)

    /**
     * Retrieves comprehensive company-wide vehicle statistics
     */
    fun getCompanyStatistics(): VehicleCompanyStatisticsResponse {
        logger.debug("Retrieving company vehicle statistics")

        return try {
            val rawStatistics = vehicleCompanyStatisticsRepository.getCompanyStatistics()

            val response = VehicleCompanyStatisticsResponse(
                totalVehicles = rawStatistics.totalVehicles,
                premiumVehicles = rawStatistics.premiumVehicles,
                visitRevenueMedian = rawStatistics.visitRevenueMedian,
                totalRevenue = rawStatistics.totalRevenue,
                averageRevenuePerVehicle = if (rawStatistics.totalVehicles > 0) {
                    rawStatistics.totalRevenue.divide(rawStatistics.totalVehicles.toBigDecimal(), 2, java.math.RoundingMode.HALF_UP)
                } else {
                    java.math.BigDecimal.ZERO
                },
                mostActiveVehicle = rawStatistics.mostActiveVehicle?.let {
                    MostActiveVehicleInfo(
                        id = it.id,
                        make = it.make,
                        model = it.model,
                        licensePlate = it.licensePlate,
                        visitCount = it.visitCount,
                        totalRevenue = it.totalRevenue
                    )
                },
                calculatedAt = LocalDateTime.now()
            )

            logger.info("Successfully calculated company statistics: ${response.totalVehicles} total vehicles, ${response.premiumVehicles} premium vehicles, median revenue: ${response.visitRevenueMedian}")
            response
        } catch (e: Exception) {
            logger.error("Error calculating company vehicle statistics", e)
            throw RuntimeException("Failed to calculate company vehicle statistics", e)
        }
    }
}