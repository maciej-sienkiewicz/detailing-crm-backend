package com.carslab.crm.production.modules.stats.domain.model

import java.math.BigDecimal

data class CategoryStatsSummary(
    val categoryId: CategoryId,
    val categoryName: String,
    val totalOrders: Long,
    val totalRevenue: BigDecimal,
    val servicesCount: Int
)