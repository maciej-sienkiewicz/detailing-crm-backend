package com.carslab.crm.production.modules.vehicles.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleStatistics
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleStatisticsRepository
import com.carslab.crm.production.modules.vehicles.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.vehicles.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
@Transactional
class VehicleStatisticsRepositoryImpl(
    private val jpaRepository: VehicleStatisticsJpaRepository
) : VehicleStatisticsRepository {

    private val logger = LoggerFactory.getLogger(VehicleStatisticsRepositoryImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByVehicleId(vehicleId: VehicleId): VehicleStatistics? {
        logger.debug("Finding statistics for vehicle: {}", vehicleId.value)

        return jpaRepository.findByVehicleId(vehicleId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByVehicleIds(vehicleIds: List<VehicleId>): List<VehicleStatistics> {
        if (vehicleIds.isEmpty()) return emptyList()

        logger.debug("Finding statistics for {} vehicles", vehicleIds.size)

        val vehicleIdValues = vehicleIds.map { it.value }
        return jpaRepository.findByVehicleIds(vehicleIdValues)
            .map { it.toDomain() }
    }

    override fun save(statistics: VehicleStatistics): VehicleStatistics {
        logger.debug("Saving statistics for vehicle: {}", statistics.vehicleId.value)

        val entity = statistics.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Statistics saved for vehicle: {}", statistics.vehicleId.value)
        return savedEntity.toDomain()
    }

    override fun incrementVisitCount(vehicleId: VehicleId) {
        logger.debug("Atomically incrementing visit count for client: {}", vehicleId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.upsertVisitCount(vehicleId.value, now, now)

        if (rowsUpdated > 0) {
            logger.debug("Visit count incremented for client: {}", vehicleId.value)
        } else {
            logger.warn("Failed to increment visit count for client: {}", vehicleId.value)
        }
    }

    override fun addRevenue(vehicleId: VehicleId, amount: BigDecimal) {
        logger.debug("Adding revenue {} for vehicle: {}", amount, vehicleId.value)

        jpaRepository.addRevenue(vehicleId.value, amount)
    }

    override fun deleteByVehicleId(vehicleId: VehicleId): Boolean {
        logger.debug("Deleting statistics for vehicle: {}", vehicleId.value)

        val deleted = jpaRepository.deleteByVehicleId(vehicleId.value)
        return deleted > 0
    }
}