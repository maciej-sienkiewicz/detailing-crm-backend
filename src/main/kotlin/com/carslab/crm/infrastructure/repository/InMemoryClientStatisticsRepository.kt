package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.ClientStatisticsRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryClientStatisticsRepository: ClientStatisticsRepository {

    private val clients = ConcurrentHashMap<Long, ClientStats>()

    override fun save(client: ClientStats): ClientStats {
        clients[client.clientId] = client
        return client
    }

    override fun findById(id: ClientId): ClientStats? {
        return clients[id.value]
    }
}