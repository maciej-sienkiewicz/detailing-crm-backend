package com.carslab.crm.production.modules.stats.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class TimeSeriesData(
    val period: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val orders: Long,
    val revenue: BigDecimal
)