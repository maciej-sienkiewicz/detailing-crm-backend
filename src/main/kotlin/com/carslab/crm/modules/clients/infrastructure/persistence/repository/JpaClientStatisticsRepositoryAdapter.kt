package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.modules.clients.domain.port.ClientStatisticsRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientStatisticsEntityDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepositoryDeprecated
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientStatisticsJpaRepositoryDeprecated
import com.carslab.crm.infrastructure.security.SecurityContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class ClientStatisticsRepositoryDeprecatedAdapter(
    private val clientStatisticsJpaRepositoryDeprecated: ClientStatisticsJpaRepositoryDeprecated,
    private val clientJpaRepositoryDeprecated: ClientJpaRepositoryDeprecated,
    private val securityContext: SecurityContext
) : ClientStatisticsRepositoryDeprecated {

    override fun save(statistics: ClientStatistics): ClientStatistics {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify client exists and belongs to company
        clientJpaRepositoryDeprecated.findByIdAndCompanyId(statistics.clientId, companyId)
            .orElse(null) ?: throw IllegalStateException("Client not found or access denied")

        val entity = if (clientStatisticsJpaRepositoryDeprecated.existsById(statistics.clientId)) {
            val existingEntity = clientStatisticsJpaRepositoryDeprecated.findByClientId(statistics.clientId).get()
            existingEntity.visitCount = statistics.visitCount
            existingEntity.totalRevenue = statistics.totalRevenue
            existingEntity.vehicleCount = statistics.vehicleCount
            existingEntity.lastVisitDate = statistics.lastVisitDate
            existingEntity
        } else {
            ClientStatisticsEntityDeprecated.fromDomain(statistics)
        }

        val savedEntity = clientStatisticsJpaRepositoryDeprecated.save(entity)
        return savedEntity.toDomain()
    }

    override fun findByClientId(clientId: ClientId): ClientStatistics? {
        val companyId = securityContext.getCurrentCompanyId()

        // Verify client exists and belongs to company
        clientJpaRepositoryDeprecated.findByIdAndCompanyId(clientId.value, companyId)
            .orElse(null) ?: return null

        return clientStatisticsJpaRepositoryDeprecated.findByClientId(clientId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun updateVisitCount(clientId: ClientId, increment: Long) {
        clientStatisticsJpaRepositoryDeprecated.updateVisitCount(clientId.value, increment, LocalDateTime.now())
    }

    override fun updateRevenue(clientId: ClientId, amount: BigDecimal) {
        clientStatisticsJpaRepositoryDeprecated.updateRevenue(clientId.value, amount, LocalDateTime.now())
    }

    override fun updateVehicleCount(clientId: ClientId, increment: Long) {
        clientStatisticsJpaRepositoryDeprecated.updateVehicleCount(clientId.value, increment, LocalDateTime.now())
    }

    override fun recalculateStatistics(clientId: ClientId): ClientStatistics {
        // Implementation for recalculating statistics
        // This would involve aggregating data from visits, vehicles, etc.
        val currentStats = findByClientId(clientId) ?: ClientStatistics(clientId = clientId.value)
        return save(currentStats)
    }

    override fun deleteByClientId(clientId: ClientId): Boolean {
        val deleted = clientStatisticsJpaRepositoryDeprecated.deleteByClientId(clientId.value)
        return deleted > 0
    }
}