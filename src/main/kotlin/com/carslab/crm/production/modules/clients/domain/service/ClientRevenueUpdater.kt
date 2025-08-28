package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ClientRevenueUpdater(
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(ClientRevenueUpdater::class.java)

    fun addRevenue(clientId: ClientId, amount: BigDecimal) {
        logger.debug("Adding client revenue for client: {}", clientId.value)

        clientStatisticsRepository.addRevenue(clientId, amount)

        logger.info("Revenue added for client: {}", clientId.value)
    }
}