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
import java.time.LocalDateTime

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

        val updatedStatistics = statistics.copy(updatedAt = LocalDateTime.now())
        val entity = updatedStatistics.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Statistics saved for client: {}", statistics.clientId.value)
        return savedEntity.toDomain()
    }

    override fun incrementVisitCount(clientId: ClientId) {
        logger.debug("Incrementing visit count for client: {}", clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.incrementVisitCount(clientId.value, now)

        if (rowsUpdated == 0) {
            logger.warn("No statistics found to increment visit count for client: {}", clientId.value)
        } else {
            logger.debug("Visit count incremented for client: {}", clientId.value)
        }
    }
    
    override fun incrementVehicleCount(clientId: ClientId) {
        logger.debug("Incrementing vehicle count for client: {}", clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.incrementVehicleCount(clientId.value, now)

        if (rowsUpdated == 0) {
            logger.warn("No statistics found to increment vehicle count for client: {}", clientId.value)
        } else {
            logger.debug("Vehicle count incremented for client: {}", clientId.value)
        }
    }

    override fun addRevenue(clientId: ClientId, amount: BigDecimal) {
        logger.debug("Adding revenue {} for client: {}", amount, clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.addRevenue(clientId.value, amount, now)

        if (rowsUpdated == 0) {
            logger.warn("No statistics found to add revenue for client: {}", clientId.value)
        } else {
            logger.debug("Revenue added for client: {}, amount: {}", clientId.value, amount)
        }
    }

    override fun incrementVehicleCount(clientId: ClientId, count: Long) {
        logger.debug("Updating vehicle count to {} for client: {}", count, clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.incrementVehicleCount(clientId.value, now)

        if (rowsUpdated == 0) {
            logger.warn("No statistics found to update vehicle count for client: {}", clientId.value)
        } else {
            logger.debug("Vehicle count updated for client: {}, count: {}", clientId.value, count)
        }
    }

    override fun setLastVisitDate(clientId: ClientId, visitDate: LocalDateTime) {
        logger.debug("Setting last visit date to {} for client: {}", visitDate, clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.setLastVisitDate(clientId.value, visitDate, now)

        if (rowsUpdated == 0) {
            logger.warn("No statistics found to set last visit date for client: {}", clientId.value)
        } else {
            logger.debug("Last visit date set for client: {}, date: {}", clientId.value, visitDate)
        }
    }

    override fun deleteByClientId(clientId: ClientId): Boolean {
        logger.debug("Deleting statistics for client: {}", clientId.value)

        val deleted = jpaRepository.deleteByClientId(clientId.value)
        val success = deleted > 0

        if (success) {
            logger.debug("Statistics deleted for client: {}", clientId.value)
        } else {
            logger.warn("No statistics found to delete for client: {}", clientId.value)
        }

        return success
    }
}