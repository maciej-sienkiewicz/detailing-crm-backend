package com.carslab.crm.production.modules.stats.domain.repository

import com.carslab.crm.production.modules.stats.application.dto.TimeGranularity
import com.carslab.crm.production.modules.stats.domain.model.*
import java.time.LocalDate

interface StatisticsRepository {
    fun getUncategorizedServices(companyId: Long): List<UncategorizedService>
    fun getCategorizedServices(categoryId: CategoryId, companyId: Long): List<CategorizedService>
    fun getCategoryStatsSummary(categoryId: CategoryId, companyId: Long): CategoryStatsSummary
    fun getCategoryStatsTimeSeries(
        categoryId: CategoryId,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity,
        companyId: Long
    ): List<TimeSeriesData>
    fun getServiceStats(
        serviceId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity,
        companyId: Long
    ): ServiceStatsData
}