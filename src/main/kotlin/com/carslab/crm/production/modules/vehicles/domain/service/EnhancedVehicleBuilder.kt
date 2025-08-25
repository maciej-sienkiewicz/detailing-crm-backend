package com.carslab.crm.production.modules.vehicles.domain.service

import com.carslab.crm.production.modules.vehicles.domain.model.*
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Component

@Component
class EnhancedVehicleBuilder(
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val vehicleOwnerResolver: VehicleOwnerResolver
) {
    private val logger = LoggerFactory.getLogger(EnhancedVehicleBuilder::class.java)

    fun buildSingle(vehicle: Vehicle, companyId: Long): EnhancedVehicle {
        logger.debug("Building single enhanced vehicle: {}", vehicle.id.value)

        val statistics = vehicleStatisticsRepository.findByVehicleId(vehicle.id)
        val owners = vehicleOwnerResolver.resolveOwnersForSingleVehicle(vehicle.id, companyId)

        return EnhancedVehicle(
            vehicle = vehicle,
            statistics = statistics,
            owners = owners
        )
    }

    fun buildMultiple(vehicles: Page<Vehicle>, companyId: Long): Page<EnhancedVehicle> {
        if (vehicles.content.isEmpty()) {
            return PageImpl(emptyList(), vehicles.pageable, vehicles.totalElements)
        }

        logger.debug("Building {} enhanced vehicles using batch loading", vehicles.content.size)

        val vehicleIds = vehicles.content.map { it.id }

        val statisticsMap = batchLoadVehicleStatistics(vehicleIds)
        val ownersMap = vehicleOwnerResolver.resolveOwners(vehicleIds, companyId)

        val enhancedVehicles = vehicles.content.map { vehicle ->
            EnhancedVehicle(
                vehicle = vehicle,
                statistics = statisticsMap[vehicle.id],
                owners = ownersMap[vehicle.id] ?: emptyList()
            )
        }

        logger.debug("Built {} enhanced vehicles using 2 batch queries total", enhancedVehicles.size)
        return PageImpl(enhancedVehicles, vehicles.pageable, vehicles.totalElements)
    }

    fun buildMultipleFromList(vehicles: List<Vehicle>, companyId: Long): List<EnhancedVehicle> {
        if (vehicles.isEmpty()) return emptyList()

        logger.debug("Building {} enhanced vehicles from list using batch loading", vehicles.size)

        val vehicleIds = vehicles.map { it.id }

        val statisticsMap = batchLoadVehicleStatistics(vehicleIds)
        val ownersMap = vehicleOwnerResolver.resolveOwners(vehicleIds, companyId)

        val enhancedVehicles = vehicles.map { vehicle ->
            EnhancedVehicle(
                vehicle = vehicle,
                statistics = statisticsMap[vehicle.id],
                owners = ownersMap[vehicle.id] ?: emptyList()
            )
        }

        logger.debug("Built {} enhanced vehicles from list using 2 batch queries total", enhancedVehicles.size)
        return enhancedVehicles
    }

    private fun batchLoadVehicleStatistics(vehicleIds: List<VehicleId>): Map<VehicleId, VehicleStatistics> {
        logger.debug("Batch loading statistics for {} vehicles", vehicleIds.size)

        return vehicleStatisticsRepository.findByVehicleIds(vehicleIds)
            .associateBy { it.vehicleId }
    }
}