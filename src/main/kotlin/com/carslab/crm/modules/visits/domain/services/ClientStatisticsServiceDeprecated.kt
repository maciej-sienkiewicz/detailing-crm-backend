package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.modules.clients.domain.port.ClientStatisticsRepositoryDeprecated
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class ClientStatisticsServiceDeprecated(
    private val clientStatisticsRepositoryDeprecated: ClientStatisticsRepositoryDeprecated
) {
    private val logger = LoggerFactory.getLogger(ClientStatisticsServiceDeprecated::class.java)

    fun updateLastVisitDate(clientId: ClientId, visitDate: LocalDateTime = LocalDateTime.now()) {
        try {
            val stats = clientStatisticsRepositoryDeprecated.findByClientId(clientId)
                ?: createInitialStatistics(clientId, visitDate)

            val updatedStats = stats.copy(lastVisitDate = visitDate)
            clientStatisticsRepositoryDeprecated.save(updatedStats)

            logger.debug("Updated last visit date for client: ${clientId.value}")
        } catch (e: Exception) {
            logger.warn("Failed to update client statistics for client: ${clientId.value}", e)
            // Graceful degradation - nie przerywamy głównego procesu
        }
    }

    private fun createInitialStatistics(clientId: ClientId, visitDate: LocalDateTime): ClientStatistics {
        return ClientStatistics(
            clientId = clientId.value,
            visitCount = 0,
            totalRevenue = BigDecimal.ZERO,
            vehicleCount = 0,
            lastVisitDate = visitDate
        )
    }
}