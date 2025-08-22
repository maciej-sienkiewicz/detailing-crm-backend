package com.carslab.crm.production.modules.stats.domain.model

import java.math.BigDecimal

data class UncategorizedService(
    private val id: ServiceId,
    private val name: String,
    private val servicesCount: Long,
    private val totalRevenue: BigDecimal
)