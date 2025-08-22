package com.carslab.crm.production.modules.stats.domain.repository

import com.carslab.crm.production.modules.stats.domain.model.UncategorizedService

interface StatisticsRepository {
    fun getUncategorizedServices(): List<UncategorizedService>
}