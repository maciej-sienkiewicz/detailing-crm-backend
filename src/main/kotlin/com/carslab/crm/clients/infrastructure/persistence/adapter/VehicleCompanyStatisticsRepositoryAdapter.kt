package com.carslab.crm.clients.infrastructure.persistence.adapter

import com.carslab.crm.clients.domain.port.VehicleCompanyStatisticsRepository
import com.carslab.crm.clients.domain.port.VehicleCompanyStatistics
import com.carslab.crm.clients.domain.port.MostActiveVehicleData
import com.carslab.crm.clients.infrastructure.persistence.repository.VehicleStatisticsJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
@Transactional(readOnly = true)
class VehicleCompanyStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepository: VehicleStatisticsJpaRepository,
    private val securityContext: SecurityContext
) : VehicleCompanyStatisticsRepository {

    override fun getCompanyStatistics(): VehicleCompanyStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        // Get total vehicle count
        val totalVehicles = vehicleStatisticsJpaRepository.countTotalVehiclesForCompany(companyId)

        // Get premium vehicle count (vehicles with 10k+ PLN in completed visits)
        val premiumVehicles = vehicleStatisticsJpaRepository.countPremiumVehiclesForCompany(companyId, BigDecimal("10000"))

        // Get visit revenue median
        val visitRevenueMedian = calculateMedianVisitRevenue(companyId)

        // Get total revenue from all completed visits
        val totalRevenue = vehicleStatisticsJpaRepository.getTotalRevenueForCompany(companyId) ?: BigDecimal.ZERO

        // Get most active vehicle
        val mostActiveVehicle = vehicleStatisticsJpaRepository.findMostActiveVehicleForCompany(companyId)
            ?.let { row ->
                MostActiveVehicleData(
                    id = row[0] as Long,
                    make = row[1] as String,
                    model = row[2] as String,
                    licensePlate = row[3] as String,
                    visitCount = (row[4] as Number).toLong(),
                    totalRevenue = (row[5] as Number).let { BigDecimal.valueOf(it.toDouble()) }
                )
            }

        return VehicleCompanyStatistics(
            totalVehicles = totalVehicles,
            premiumVehicles = premiumVehicles,
            visitRevenueMedian = visitRevenueMedian,
            totalRevenue = totalRevenue,
            mostActiveVehicle = mostActiveVehicle
        )
    }

    private fun calculateMedianVisitRevenue(companyId: Long): BigDecimal {
        val revenues = vehicleStatisticsJpaRepository.getAllCompletedVisitRevenuesForCompany(companyId)

        return if (revenues.isEmpty()) {
            BigDecimal.ZERO
        } else {
            val sortedRevenues = revenues.sorted()
            val size = sortedRevenues.size

            if (size % 2 == 0) {
                // Even number of elements - take average of two middle elements
                val mid1 = sortedRevenues[size / 2 - 1]
                val mid2 = sortedRevenues[size / 2]
                mid1.add(mid2).divide(BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP)
            } else {
                // Odd number of elements - take middle element
                sortedRevenues[size / 2]
            }
        }
    }
}