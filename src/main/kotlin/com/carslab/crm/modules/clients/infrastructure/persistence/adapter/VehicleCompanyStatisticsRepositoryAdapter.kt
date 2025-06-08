package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.port.VehicleCompanyStatisticsRepository
import com.carslab.crm.modules.clients.domain.port.VehicleCompanyStatistics
import com.carslab.crm.modules.clients.domain.port.MostActiveVehicleData
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleStatisticsJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import org.slf4j.LoggerFactory

@Repository
@Transactional(readOnly = true)
class VehicleCompanyStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepository: VehicleStatisticsJpaRepository,
    private val securityContext: SecurityContext
) : VehicleCompanyStatisticsRepository {

    private val logger = LoggerFactory.getLogger(VehicleCompanyStatisticsRepositoryAdapter::class.java)

    override fun getCompanyStatistics(): VehicleCompanyStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        logger.debug("Retrieving company vehicle statistics for companyId: $companyId")

        return try {
            // Get total vehicle count with error handling
            val totalVehicles = try {
                vehicleStatisticsJpaRepository.countTotalVehiclesForCompany(companyId)
            } catch (e: Exception) {
                logger.warn("Error getting total vehicles count: ${e.message}")
                0L
            }

            logger.debug("Total vehicles: $totalVehicles")

            // Get premium vehicle count (vehicles with 10k+ PLN in completed visits)
            val premiumVehicles = try {
                vehicleStatisticsJpaRepository.countPremiumVehiclesForCompany(companyId, BigDecimal("10000"))
            } catch (e: Exception) {
                logger.warn("Error getting premium vehicles count: ${e.message}")
                0L
            }

            logger.debug("Premium vehicles: $premiumVehicles")

            // Get visit revenue median with improved error handling
            val visitRevenueMedian = try {
                calculateMedianVisitRevenue(companyId)
            } catch (e: Exception) {
                logger.warn("Error calculating visit revenue median: ${e.message}")
                BigDecimal.ZERO
            }

            logger.debug("Visit revenue median: $visitRevenueMedian")

            // Get total revenue from all completed visits
            val totalRevenue = try {
                vehicleStatisticsJpaRepository.getTotalRevenueFromProtocolServicesForCompany(companyId) ?: BigDecimal.ZERO
            } catch (e: Exception) {
                logger.warn("Error getting total revenue: ${e.message}")
                BigDecimal.ZERO
            }

            logger.debug("Total revenue: $totalRevenue")

            // Get most active vehicle with improved error handling
            val mostActiveVehicle = try {
                getMostActiveVehicle(companyId)
            } catch (e: Exception) {
                logger.warn("Error getting most active vehicle: ${e.message}")
                null
            }

            logger.debug("Most active vehicle: ${mostActiveVehicle?.let { "${it.make} ${it.model} (${it.licensePlate})" } ?: "None"}")

            val result = VehicleCompanyStatistics(
                totalVehicles = totalVehicles,
                premiumVehicles = premiumVehicles,
                visitRevenueMedian = visitRevenueMedian,
                totalRevenue = totalRevenue,
                mostActiveVehicle = mostActiveVehicle
            )

            logger.info("Successfully calculated company statistics: ${result.totalVehicles} total vehicles, ${result.premiumVehicles} premium vehicles")

            result

        } catch (e: Exception) {
            logger.error("Fatal error calculating company vehicle statistics for companyId: $companyId", e)

            // Return safe default values instead of throwing
            VehicleCompanyStatistics(
                totalVehicles = 0L,
                premiumVehicles = 0L,
                visitRevenueMedian = BigDecimal.ZERO,
                totalRevenue = BigDecimal.ZERO,
                mostActiveVehicle = null
            )
        }
    }

    /**
     * POPRAWIONA: Bardziej niezawodne obliczanie mediany z lepszą obsługą błędów
     */
    private fun calculateMedianVisitRevenue(companyId: Long): BigDecimal {
        return try {
            val revenues = vehicleStatisticsJpaRepository.getAllCompletedVisitRevenuesForCompany(companyId)

            if (revenues.isEmpty()) {
                logger.debug("No revenue data found for median calculation")
                return BigDecimal.ZERO
            }

            logger.debug("Calculating median from ${revenues.size} revenue entries")

            val sortedRevenues = revenues
                .filter { it > BigDecimal.ZERO } // Filter out zero or negative values
                .sorted()

            if (sortedRevenues.isEmpty()) {
                logger.debug("No positive revenue values found for median calculation")
                return BigDecimal.ZERO
            }

            val size = sortedRevenues.size

            val median = if (size % 2 == 0) {
                // Even number of elements - take average of two middle elements
                val mid1 = sortedRevenues[size / 2 - 1]
                val mid2 = sortedRevenues[size / 2]
                mid1.add(mid2).divide(BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP)
            } else {
                // Odd number of elements - take middle element
                sortedRevenues[size / 2]
            }

            logger.debug("Calculated median: $median from $size values")
            median

        } catch (e: Exception) {
            logger.error("Error calculating median visit revenue for companyId: $companyId", e)
            BigDecimal.ZERO
        }
    }

    /**
     * NOWA: Bezpieczne pobieranie informacji o najaktywniejszym pojeździe
     */
    private fun getMostActiveVehicle(companyId: Long): MostActiveVehicleData? {
        return try {
            val result = vehicleStatisticsJpaRepository.findMostActiveVehicleForCompany(companyId)

            if (result == null) {
                logger.debug("No active vehicle found for companyId: $companyId")
                return null
            }

            // Validate array structure
            if (result.size < 6) {
                logger.warn("Invalid result array size for most active vehicle: expected 6, got ${result.size}")
                return null
            }

            MostActiveVehicleData(
                id = convertToLong(result[0]),
                make = result[1] as String,
                model = result[2] as String,
                licensePlate = result[3] as String,
                visitCount = convertToLong(result[4]),
                totalRevenue = convertToDecimal(result[5])
            )

        } catch (e: Exception) {
            logger.error("Error getting most active vehicle for companyId: $companyId", e)
            null
        }
    }

    /**
     * NOWA: Bezpieczna konwersja na BigDecimal z obsługą różnych typów
     */
    private fun convertToDecimal(value: Any): BigDecimal {
        return when (value) {
            is BigDecimal -> value
            is Number -> BigDecimal.valueOf(value.toDouble())
            is String -> value.toBigDecimalOrNull() ?: BigDecimal.ZERO
            else -> {
                logger.warn("Unexpected value type for decimal conversion: ${value::class.simpleName}")
                BigDecimal.ZERO
            }
        }
    }

    /**
     * NOWA: Bezpieczna konwersja na Long
     */
    private fun convertToLong(value: Any): Long {
        return when (value) {
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> {
                logger.warn("Unexpected value type for long conversion: ${value::class.simpleName}")
                0L
            }
        }
    }
}