package com.carslab.crm.modules.clients.domain.port

import com.carslab.crm.modules.clients.api.responses.VehicleTableResponse
import com.carslab.crm.modules.clients.domain.VehicleTableSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

/**
 * Repository port for vehicle table operations
 */
interface VehicleTableRepository {
    /**
     * Finds vehicles with all necessary data for table display including owners and statistics
     */
    fun findVehiclesForTable(
        criteria: VehicleTableSearchCriteria,
        pageable: Pageable
    ): Page<VehicleTableResponse>
}

/**
 * Repository port for company-wide vehicle statistics
 */
interface VehicleCompanyStatisticsRepository {
    /**
     * Calculates comprehensive company statistics for vehicles
     */
    fun getCompanyStatistics(): VehicleCompanyStatistics
}

/**
 * Raw statistics data from repository
 */
data class VehicleCompanyStatistics(
    val totalVehicles: Long,
    val premiumVehicles: Long,
    val visitRevenueMedian: BigDecimal,
    val totalRevenue: BigDecimal,
    val mostActiveVehicle: MostActiveVehicleData?
)

/**
 * Raw data for most active vehicle
 */
data class MostActiveVehicleData(
    val id: Long,
    val make: String,
    val model: String,
    val licensePlate: String,
    val visitCount: Long,
    val totalRevenue: BigDecimal
)