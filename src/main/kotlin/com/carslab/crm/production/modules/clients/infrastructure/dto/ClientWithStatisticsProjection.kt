package com.carslab.crm.production.modules.clients.infrastructure.dto

import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientEntity
import com.carslab.crm.production.modules.clients.infrastructure.entity.ClientStatisticsEntity

interface ClientWithStatisticsProjection {
    fun getClient(): ClientEntity
    fun getStatistics(): ClientStatisticsEntity?
}