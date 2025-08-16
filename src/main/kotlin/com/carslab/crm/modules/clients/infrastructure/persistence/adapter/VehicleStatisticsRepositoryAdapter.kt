package com.carslab.crm.modules.clients.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.clients.domain.model.VehicleStatistics
import com.carslab.crm.modules.clients.domain.port.VehicleStatisticsRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleStatisticsEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleStatisticsJpaRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepositoryDeprecated
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class VehicleStatisticsRepositoryAdapter(
    private val vehicleStatisticsJpaRepositoryDeprecated: VehicleStatisticsJpaRepositoryDeprecated,
    private val vehicleJpaRepositoryDeprecated: VehicleJpaRepositoryDeprecated,
    private val securityContext: SecurityContext
) : VehicleStatisticsRepositoryDeprecated {

    override fun save(statistics: VehicleStatistics): VehicleStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify vehicle exists and belongs to company
        vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(statistics.vehicleId, companyId)
            .orElse(null) ?: throw IllegalStateException("Vehicle not found or access denied")

        val entity = if (vehicleStatisticsJpaRepositoryDeprecated.existsById(statistics.vehicleId)) {
            val existingEntity = vehicleStatisticsJpaRepositoryDeprecated.findByVehicleId(statistics.vehicleId).get()
            existingEntity.visitCount = statistics.visitCount
            existingEntity.totalRevenue = statistics.totalRevenue
            existingEntity.lastVisitDate = statistics.lastVisitDate
            existingEntity.updatedAt = LocalDateTime.now()
            existingEntity
        } else {
            VehicleStatisticsEntityDeprecated.fromDomain(statistics)
        }

        val savedEntity = vehicleStatisticsJpaRepositoryDeprecated.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByVehicleId(vehicleId: VehicleId): VehicleStatistics? {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify vehicle exists and belongs to company
        vehicleJpaRepositoryDeprecated.findByIdAndCompanyId(vehicleId.value, companyId)
            .orElse(null) ?: return null

        return vehicleStatisticsJpaRepositoryDeprecated.findByVehicleId(vehicleId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun updateVisitCount(vehicleId: VehicleId, increment: Long) {
        vehicleStatisticsJpaRepositoryDeprecated.updateVisitCount(vehicleId.value, increment, LocalDateTime.now())
    }

    override fun updateRevenue(vehicleId: VehicleId, amount: BigDecimal) {
        vehicleStatisticsJpaRepositoryDeprecated.updateRevenue(vehicleId.value, amount, LocalDateTime.now())
    }

    override fun recalculateStatistics(vehicleId: VehicleId): VehicleStatistics {
        val currentStats = findByVehicleId(vehicleId) ?: VehicleStatistics(vehicleId = vehicleId.value)
        return save(currentStats)
    }

    override fun deleteByVehicleId(vehicleId: VehicleId): Boolean {
        val deleted = vehicleStatisticsJpaRepositoryDeprecated.deleteByVehicleId(vehicleId.value)
        return deleted > 0
    }
}