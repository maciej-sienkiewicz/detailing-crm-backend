package com.carslab.crm.production.modules.stats.domain.model

import java.math.BigDecimal

data class CategorizedService(
    val id: String,
    val name: String,
    val servicesCount: Long,
    val totalRevenue: BigDecimal
)