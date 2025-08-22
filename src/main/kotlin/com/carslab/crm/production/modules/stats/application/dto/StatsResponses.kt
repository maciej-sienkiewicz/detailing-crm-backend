package com.carslab.crm.production.modules.stats.application.dto

import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.UncategorizedService
import java.math.BigDecimal

data class CategoryResponse(
    val id: Long,
    val name: String,
    val servicesCount: Int
) {
    companion object {
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id.id,
                name = category.name,
                servicesCount = category.servicesCount
            )
        }
    }
}

data class UncategorizedServiceResponse(
    val id: Long,
    val name: String,
    val servicesCount: Long,
    val totalRevenue: BigDecimal
) {
    companion object {
        fun from(service: UncategorizedService): UncategorizedServiceResponse {
            return UncategorizedServiceResponse(
                id = service.id.id,
                name = service.name,
                servicesCount = service.servicesCount,
                totalRevenue = service.totalRevenue
            )
        }
    }
}