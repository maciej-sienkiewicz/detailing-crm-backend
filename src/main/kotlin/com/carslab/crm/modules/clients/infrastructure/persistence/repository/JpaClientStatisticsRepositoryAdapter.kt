package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.modules.clients.domain.port.ClientStatisticsRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientStatisticsEntity
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientStatisticsJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class ClientStatisticsRepositoryAdapter(
    private val clientStatisticsJpaRepository: ClientStatisticsJpaRepository,
    private val clientJpaRepository: ClientJpaRepository,
    private val securityContext: SecurityContext
) : ClientStatisticsRepository {

    override fun save(statistics: ClientStatistics): ClientStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify client exists and belongs to company
        clientJpaRepository.findByIdAndCompanyId(statistics.clientId, companyId)
            .orElse(null) ?: throw IllegalStateException("Client not found or access denied")

        val entity = if (clientStatisticsJpaRepository.existsById(statistics.clientId)) {
            val existingEntity = clientStatisticsJpaRepository.findByClientId(statistics.clientId).get()
            existingEntity.visitCount = statistics.visitCount
            existingEntity.totalRevenue = statistics.totalRevenue
            existingEntity.vehicleCount = statistics.vehicleCount
            existingEntity.lastVisitDate = statistics.lastVisitDate
            existingEntity
        } else {
            ClientStatisticsEntity.fromDomain(statistics)
        }

        val savedEntity = clientStatisticsJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByClientId(clientId: ClientId): ClientStatistics? {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify client exists and belongs to company
        clientJpaRepository.findByIdAndCompanyId(clientId.value, companyId)
            .orElse(null) ?: return null

        return clientStatisticsJpaRepository.findByClientId(clientId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun updateVisitCount(clientId: ClientId, increment: Long) {
        clientStatisticsJpaRepository.updateVisitCount(clientId.value, increment, LocalDateTime.now())
    }

    override fun updateRevenue(clientId: ClientId, amount: BigDecimal) {
        clientStatisticsJpaRepository.updateRevenue(clientId.value, amount, LocalDateTime.now())
    }

    override fun updateVehicleCount(clientId: ClientId, increment: Long) {
        clientStatisticsJpaRepository.updateVehicleCount(clientId.value, increment, LocalDateTime.now())
    }

    override fun recalculateStatistics(clientId: ClientId): ClientStatistics {
        // Implementation for recalculating statistics
        // This would involve aggregating data from visits, vehicles, etc.
        val currentStats = findByClientId(clientId) ?: ClientStatistics(clientId = clientId.value)
        return save(currentStats)
    }

    override fun deleteByClientId(clientId: ClientId): Boolean {
        val deleted = clientStatisticsJpaRepository.deleteByClientId(clientId.value)
        return deleted > 0
    }
}