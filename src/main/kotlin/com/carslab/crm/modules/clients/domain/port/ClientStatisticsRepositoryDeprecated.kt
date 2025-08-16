package com.carslab.crm.modules.clients.domain.port

import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.ClientStatistics
import java.math.BigDecimal

interface ClientStatisticsRepositoryDeprecated {
    fun findByClientId(clientId: ClientId): ClientStatistics?
    fun save(statistics: ClientStatistics): ClientStatistics
    fun updateVisitCount(clientId: ClientId, increment: Long)
    fun updateRevenue(clientId: ClientId, amount: BigDecimal)
    fun updateVehicleCount(clientId: ClientId, increment: Long)
    fun recalculateStatistics(clientId: ClientId): ClientStatistics
    fun deleteByClientId(clientId: ClientId): Boolean
}