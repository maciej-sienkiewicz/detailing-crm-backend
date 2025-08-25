package com.carslab.crm.production.modules.clients.domain.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.modules.clients.domain.repository.ClientStatisticsRepository
import org.springframework.stereotype.Component

@Component
class ClientStatisticsInitializer(
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    fun initialize(clientId: ClientId) {
        val statistics = ClientStatistics(clientId = clientId)
        clientStatisticsRepository.save(statistics)
    }
}