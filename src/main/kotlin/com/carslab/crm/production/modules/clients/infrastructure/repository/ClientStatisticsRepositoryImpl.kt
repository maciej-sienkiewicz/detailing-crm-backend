package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
@Transactional
class ClientStatisticsRepositoryImpl(
    private val jpaRepository: ClientStatisticsJpaRepository
) : ClientStatisticsRepository {

    private val logger = LoggerFactory.getLogger(ClientStatisticsRepositoryImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByClientId(clientId: ClientId): ClientStatistics? {
        logger.debug("Finding statistics for client: {}", clientId.value)

        return jpaRepository.findByClientId(clientId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun save(statistics: ClientStatistics): ClientStatistics {
        logger.debug("Saving statistics for client: {}", statistics.clientId.value)

        val entity = statistics.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Statistics saved for client: {}", statistics.clientId.value)
        return savedEntity.toDomain()
    }

    override fun incrementVisitCount(clientId: ClientId) {
        logger.debug("Incrementing visit count for client: {}", clientId.value)

        jpaRepository.incrementVisitCount(clientId.value)
    }

    override fun addRevenue(clientId: ClientId, amount: BigDecimal) {
        logger.debug("Adding revenue {} for client: {}", amount, clientId.value)

        jpaRepository.addRevenue(clientId.value, amount)
    }

    override fun updateVehicleCount(clientId: ClientId, count: Long) {
        logger.debug("Updating vehicle count to {} for client: {}", count, clientId.value)

        jpaRepository.updateVehicleCount(clientId.value, count)
    }

    override fun deleteByClientId(clientId: ClientId): Boolean {
        logger.debug("Deleting statistics for client: {}", clientId.value)

        val deleted = jpaRepository.deleteByClientId(clientId.value)
        return deleted > 0
    }
}