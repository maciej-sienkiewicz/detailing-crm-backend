package com.carslab.crm.production.modules.clients.domain.repository

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import java.math.BigDecimal

interface ClientStatisticsRepository {
    fun findByClientId(clientId: ClientId): ClientStatistics?
    fun save(statistics: ClientStatistics): ClientStatistics
    fun incrementVisitCount(clientId: ClientId)
    fun addRevenue(clientId: ClientId, amount: BigDecimal)
    fun updateVehicleCount(clientId: ClientId, count: Long)
    fun deleteByClientId(clientId: ClientId): Boolean
}