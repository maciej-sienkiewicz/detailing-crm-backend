package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ClientVehicleCounter(
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    private val logger = LoggerFactory.getLogger(ClientVehicleCounter::class.java)

    fun incrementVehicleCount(clientId: ClientId) {
        logger.debug("Increasing vehicle count for client: {}", clientId.value)

        clientStatisticsRepository.incrementVehicleCount(clientId)

        logger.info("Vehicle count increased for client: {}", clientId.value)
    }
    
    fun decrementVehicleCount(clientId: ClientId) {
        logger.debug("Decreasing vehicle count for client: {}", clientId.value)

        clientStatisticsRepository.decrementVehicleCount(clientId)

        logger.info("Vehicle count decreased for client: {}", clientId.value)
    }
}