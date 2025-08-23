package com.carslab.crm.production.modules.stats.application.service

import com.carslab.crm.production.modules.stats.application.dto.CategorizedServiceResponse
import com.carslab.crm.production.modules.stats.application.dto.CategoryStatsRequest
import com.carslab.crm.production.modules.stats.application.dto.CategoryStatsSummaryResponse
import com.carslab.crm.production.modules.stats.application.dto.CategoryStatsTimeSeriesResponse
import com.carslab.crm.production.modules.stats.application.dto.ServiceStatsRequest
import com.carslab.crm.production.modules.stats.application.dto.ServiceStatsResponse
import com.carslab.crm.production.modules.stats.application.dto.UncategorizedServiceResponse
import com.carslab.crm.production.modules.stats.domain.StatsService
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StatsQueryService(
    private val statsService: StatsService
) {
    private val logger = LoggerFactory.getLogger(StatsQueryService::class.java)

    fun getUncategorizedServices(): List<UncategorizedServiceResponse> {
        logger.debug("Fetching uncategorized services")

        val services = statsService.getUncategorizedServices()

        logger.debug("Found {} uncategorized services", services.size)
        return services.map { UncategorizedServiceResponse.from(it) }
    }
    
    fun getCategoriesWithServiceCounts(): List<com.carslab.crm.production.modules.stats.application.dto.CategoryResponse> {
        logger.debug("Fetching categories with service counts")

        val categories = statsService.getCategoriesWithServiceCounts()

        logger.debug("Found {} categories", categories.size)
        return categories.map { com.carslab.crm.production.modules.stats.application.dto.CategoryResponse.from(it) }
    }

    fun getCategoryStatsSummary(categoryId: Long): CategoryStatsSummaryResponse {
        logger.debug("Fetching category stats summary for category: {}", categoryId)

        val summary = statsService.getCategoryStatsSummary(CategoryId(categoryId))

        logger.debug("Retrieved summary for category: {}", categoryId)
        return CategoryStatsSummaryResponse.from(summary)
    }

    fun getCategoryStatsTimeSeries(categoryId: Long, request: CategoryStatsRequest): CategoryStatsTimeSeriesResponse {
        logger.debug("Fetching category time series stats for category: {} from {} to {} with granularity: {}",
            categoryId, request.startDate, request.endDate, request.granularity)

        val categoryName = statsService.getCategoryName(CategoryId(categoryId))
        val timeSeries = statsService.getCategoryStatsTimeSeries(
            CategoryId(categoryId),
            request.startDate,
            request.endDate,
            request.granularity
        )

        logger.debug("Retrieved {} time series data points for category: {}", timeSeries.size, categoryId)
        return CategoryStatsTimeSeriesResponse.from(categoryId, categoryName, request.granularity, timeSeries)
    }

    fun getServiceStats(request: ServiceStatsRequest): ServiceStatsResponse {
        logger.debug("Fetching service stats for service: {} from {} to {} with granularity: {}",
            request.serviceId, request.startDate, request.endDate, request.granularity)

        val serviceStats = statsService.getServiceStats(
            request.serviceId,
            request.startDate,
            request.endDate,
            request.granularity
        )

        logger.debug("Retrieved {} time series data points for service: {}", serviceStats.data.size, request.serviceId)
        return ServiceStatsResponse.from(serviceStats)
    }

    fun getCategorizedServices(categoryId: Long): List<CategorizedServiceResponse> {
        logger.debug("Fetching services for category: {}", categoryId)

        val services = statsService.getCategorizedServices(CategoryId(categoryId))
        
        logger.debug("Found {} services in category: {}", services.size, categoryId)
        return services.map { CategorizedServiceResponse.from(it) }
    }
}
