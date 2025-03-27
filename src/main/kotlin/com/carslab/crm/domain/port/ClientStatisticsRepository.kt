package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.model.stats.ClientStats

interface ClientStatisticsRepository {

    fun save(client: ClientStats): ClientStats
    fun findById(id: ClientId): ClientStats?
}