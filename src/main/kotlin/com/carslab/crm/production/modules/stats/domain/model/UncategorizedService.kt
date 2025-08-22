package com.carslab.crm.production.modules.stats.domain.model

import java.math.BigDecimal

data class UncategorizedService(
    val id: ServiceId,
    val name: String,
    val servicesCount: Long,
    val totalRevenue: BigDecimal
)