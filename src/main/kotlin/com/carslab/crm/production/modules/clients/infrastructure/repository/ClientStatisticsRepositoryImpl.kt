package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toDomain
import com.carslab.crm.production.modules.clients.infrastructure.mapper.toEntity
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
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
    @DatabaseMonitored(repository = "client_statistics", method = "findByClientId", operation = "select")
    override fun findByClientId(clientId: ClientId): ClientStatistics? {
        logger.debug("Finding statistics for client: {}", clientId.value)

        return jpaRepository.findByClientId(clientId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    @DatabaseMonitored(repository = "client_statistics", method = "save", operation = "insert")
    override fun save(statistics: ClientStatistics): ClientStatistics {
        logger.debug("Saving statistics for client: {}", statistics.clientId.value)

        val updatedStatistics = statistics.copy(updatedAt = LocalDateTime.now())
        val entity = updatedStatistics.toEntity()
        val savedEntity = jpaRepository.save(entity)

        logger.debug("Statistics saved for client: {}", statistics.clientId.value)
        return savedEntity.toDomain()
    }

    @DatabaseMonitored(repository = "client_statistics", method = "incrementVisitCount", operation = "update")
    override fun incrementVisitCount(clientId: ClientId) {
        logger.debug("Atomically incrementing visit count for client: {}", clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.upsertVisitCount(clientId.value, now, now)

        if (rowsUpdated > 0) {
            logger.debug("Visit count incremented for client: {}", clientId.value)
        } else {
            logger.warn("Failed to increment visit count for client: {}", clientId.value)
        }
    }

    @DatabaseMonitored(repository = "client_statistics", method = "incrementVehicleCount", operation = "update")
    override fun incrementVehicleCount(clientId: ClientId) {
        logger.debug("Atomically incrementing vehicle count for client: {}", clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.upsertVehicleCount(clientId.value, now)

        if (rowsUpdated > 0) {
            logger.debug("Vehicle count incremented for client: {}", clientId.value)
        } else {
            logger.warn("Failed to increment vehicle count for client: {}", clientId.value)
        }
    }

    @DatabaseMonitored(repository = "client_statistics", method = "addRevenue", operation = "update")
    override fun addRevenue(clientId: ClientId, amount: BigDecimal) {
        logger.debug("Atomically adding revenue {} for client: {}", amount, clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.upsertRevenue(clientId.value, amount, now)

        if (rowsUpdated > 0) {
            logger.debug("Revenue added for client: {}, amount: {}", clientId.value, amount)
        } else {
            logger.warn("Failed to add revenue for client: {}", clientId.value)
        }
    }

    @DatabaseMonitored(repository = "client_statistics", method = "decrementVehicleCount", operation = "update")
    override fun decrementVehicleCount(clientId: ClientId) {
        logger.debug("Atomically decrementing vehicle count for client: {}", clientId.value)

        val now = LocalDateTime.now()
        val rowsUpdated = jpaRepository.decrementVehicleCount(clientId.value, now)

        if (rowsUpdated > 0) {
            logger.debug("Vehicle count decremented for client: {}", clientId.value)
        } else {
            logger.warn("Failed to decrement vehicle count for client: {}", clientId.value)
        }
    }

    @DatabaseMonitored(repository = "client_statistics", method = "setLastVisitDate", operation = "update")
    override fun setLastVisitDate(clientId: ClientId, visitDate: LocalDateTime) {
        logger.debug("Setting last visit date atomically through upsertVisitCount for client: {}", clientId.value)
    }

    @DatabaseMonitored(repository = "client_statistics", method = "deleteByClientId", operation = "delete")
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

    @DatabaseMonitored(repository = "client_statistics", method = "batchDelete", operation = "delete")
    fun batchDelete(clientIds: List<ClientId>) {
        if (clientIds.isEmpty()) return

        logger.debug("Batch deleting statistics for {} clients", clientIds.size)

        val clientIdValues = clientIds.map { it.value }
        val deleted = jpaRepository.deleteByClientIds(clientIdValues)

        logger.debug("Batch deleted {} statistics records", deleted)
    }

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "client_statistics", method = "findByClientIds", operation = "select")
    fun findByClientIds(clientIds: List<ClientId>): Map<ClientId, ClientStatistics> {
        if (clientIds.isEmpty()) return emptyMap()

        logger.debug("Batch finding statistics for {} clients", clientIds.size)

        val clientIdValues = clientIds.map { it.value }
        return jpaRepository.findByClientIds(clientIdValues)
            .map { it.toDomain() }
            .associateBy { it.clientId }
    }
}