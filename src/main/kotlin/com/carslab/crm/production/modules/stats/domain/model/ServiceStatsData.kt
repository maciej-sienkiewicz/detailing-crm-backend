package com.carslab.crm.production.modules.stats.domain.model

import com.carslab.crm.production.modules.stats.application.dto.TimeGranularity

data class ServiceStatsData(
    val serviceId: String,
    val serviceName: String,
    val granularity: TimeGranularity,
    val data: List<TimeSeriesData>
)