package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleStatistics
import com.carslab.crm.modules.clients.domain.port.VehicleStatisticsRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleStatisticsEntity
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleStatisticsJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class VehicleStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepository: VehicleStatisticsJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val securityContext: SecurityContext
) : VehicleStatisticsRepository {

    override fun save(statistics: VehicleStatistics): VehicleStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify vehicle exists and belongs to company
        vehicleJpaRepository.findByIdAndCompanyId(statistics.vehicleId, companyId)
            .orElse(null) ?: throw IllegalStateException("Vehicle not found or access denied")

        val entity = if (vehicleStatisticsJpaRepository.existsById(statistics.vehicleId)) {
            val existingEntity = vehicleStatisticsJpaRepository.findByVehicleId(statistics.vehicleId).get()
            existingEntity.visitCount = statistics.visitCount
            existingEntity.totalRevenue = statistics.totalRevenue
            existingEntity.lastVisitDate = statistics.lastVisitDate
            existingEntity.updatedAt = LocalDateTime.now()
            existingEntity
        } else {
            VehicleStatisticsEntity.fromDomain(statistics)
        }

        val savedEntity = vehicleStatisticsJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByVehicleId(vehicleId: VehicleId): VehicleStatistics? {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify vehicle exists and belongs to company
        vehicleJpaRepository.findByIdAndCompanyId(vehicleId.value, companyId)
            .orElse(null) ?: return null

        return vehicleStatisticsJpaRepository.findByVehicleId(vehicleId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun updateVisitCount(vehicleId: VehicleId, increment: Long) {
        vehicleStatisticsJpaRepository.updateVisitCount(vehicleId.value, increment, LocalDateTime.now())
    }

    override fun updateRevenue(vehicleId: VehicleId, amount: BigDecimal) {
        vehicleStatisticsJpaRepository.updateRevenue(vehicleId.value, amount, LocalDateTime.now())
    }

    override fun recalculateStatistics(vehicleId: VehicleId): VehicleStatistics {
        val currentStats = findByVehicleId(vehicleId) ?: VehicleStatistics(vehicleId = vehicleId.value)
        return save(currentStats)
    }

    override fun deleteByVehicleId(vehicleId: VehicleId): Boolean {
        val deleted = vehicleStatisticsJpaRepository.deleteByVehicleId(vehicleId.value)
        return deleted > 0
    }
}