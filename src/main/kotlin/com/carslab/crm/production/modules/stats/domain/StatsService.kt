package com.carslab.crm.production.modules.stats.domain

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.stats.application.dto.TimeGranularity
import com.carslab.crm.production.modules.stats.domain.model.*
import com.carslab.crm.production.modules.stats.domain.repository.CategoriesRepository
import com.carslab.crm.production.modules.stats.domain.repository.StatisticsRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsService(
    private val statisticsRepository: StatisticsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val securityContext: SecurityContext
) {

    fun createCategory(name: String): Category =
        categoriesRepository.createCategory(name, securityContext.getCurrentCompanyId())

    fun addToCategory(services: List<ServiceId>, categoryId: CategoryId) {
        categoriesRepository.addToCategory(services, categoryId)
    }

    fun getUncategorizedServices(): List<UncategorizedService> =
        statisticsRepository.getUncategorizedServices(securityContext.getCurrentCompanyId())

    fun getCategorizedServices(categoryId: CategoryId): List<CategorizedService> =
        statisticsRepository.getCategorizedServices(categoryId, securityContext.getCurrentCompanyId())

    fun getCategoriesWithServiceCounts(): List<Category> =
        categoriesRepository.getCategories(securityContext.getCurrentCompanyId())

    fun getCategoryStatsSummary(categoryId: CategoryId): CategoryStatsSummary =
        statisticsRepository.getCategoryStatsSummary(categoryId, securityContext.getCurrentCompanyId())

    fun getCategoryStatsTimeSeries(
        categoryId: CategoryId,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity
    ): List<TimeSeriesData> =
        statisticsRepository.getCategoryStatsTimeSeries(
            categoryId,
            startDate,
            endDate,
            granularity,
            securityContext.getCurrentCompanyId()
        )

    fun getServiceStats(
        serviceId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity
    ): ServiceStatsData =
        statisticsRepository.getServiceStats(
            serviceId,
            startDate,
            endDate,
            granularity,
            securityContext.getCurrentCompanyId()
        )

    fun getCategoryName(categoryId: CategoryId): String =
        categoriesRepository.getCategoryName(categoryId, securityContext.getCurrentCompanyId())
}
