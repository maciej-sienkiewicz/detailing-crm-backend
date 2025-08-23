package com.carslab.crm.production.modules.stats.application.dto

import com.carslab.crm.production.modules.stats.domain.model.CategorizedService
import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.CategoryStatsSummary
import com.carslab.crm.production.modules.stats.domain.model.ServiceStatsData
import com.carslab.crm.production.modules.stats.domain.model.TimeSeriesData
import com.carslab.crm.production.modules.stats.domain.model.UncategorizedService
import java.math.BigDecimal
import java.time.LocalDate

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
    val id: String,
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

data class CategorizedServiceResponse(
    val id: String,
    val name: String,
    val servicesCount: Long,
    val totalRevenue: BigDecimal
) {
    companion object {
        fun from(service: CategorizedService): CategorizedServiceResponse {
            return CategorizedServiceResponse(
                id = service.id,
                name = service.name,
                servicesCount = service.servicesCount,
                totalRevenue = service.totalRevenue
            )
        }
    }
}

data class CategoryStatsSummaryResponse(
    val categoryId: Long,
    val categoryName: String,
    val totalOrders: Long,
    val totalRevenue: BigDecimal,
    val servicesCount: Int
) {
    companion object {
        fun from(stats: CategoryStatsSummary): CategoryStatsSummaryResponse {
            return CategoryStatsSummaryResponse(
                categoryId = stats.categoryId.id,
                categoryName = stats.categoryName,
                totalOrders = stats.totalOrders,
                totalRevenue = stats.totalRevenue,
                servicesCount = stats.servicesCount
            )
        }
    }
}

data class CategoryStatsTimeSeriesResponse(
    val categoryId: Long,
    val categoryName: String,
    val granularity: TimeGranularity,
    val data: List<TimeSeriesDataResponse>
) {
    companion object {
        fun from(categoryId: Long, categoryName: String, granularity: TimeGranularity, data: List<TimeSeriesData>): CategoryStatsTimeSeriesResponse {
            return CategoryStatsTimeSeriesResponse(
                categoryId = categoryId,
                categoryName = categoryName,
                granularity = granularity,
                data = data.map { TimeSeriesDataResponse.from(it) }
            )
        }
    }
}

data class ServiceStatsResponse(
    val serviceId: String,
    val serviceName: String,
    val granularity: TimeGranularity,
    val data: List<TimeSeriesDataResponse>
) {
    companion object {
        fun from(stats: ServiceStatsData): ServiceStatsResponse {
            return ServiceStatsResponse(
                serviceId = stats.serviceId,
                serviceName = stats.serviceName,
                granularity = stats.granularity,
                data = stats.data.map { TimeSeriesDataResponse.from(it) }
            )
        }
    }
}

data class TimeSeriesDataResponse(
    val period: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val orders: Long,
    val revenue: BigDecimal
) {
    companion object {
        fun from(data: TimeSeriesData): TimeSeriesDataResponse {
            return TimeSeriesDataResponse(
                period = data.period,
                periodStart = data.periodStart,
                periodEnd = data.periodEnd,
                orders = data.orders,
                revenue = data.revenue
            )
        }
    }
}