package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.application.dto.TimeGranularity
import com.carslab.crm.production.modules.stats.domain.model.*
import com.carslab.crm.production.modules.stats.domain.repository.StatisticsRepository
import com.carslab.crm.production.modules.stats.infrastructure.mapper.TimeSeriesMapper
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class StatisticsRepositoryImpl(
    private val statisticsJpaRepository: StatisticsJpaRepository
) : StatisticsRepository {

    private val logger = LoggerFactory.getLogger(StatisticsRepositoryImpl::class.java)

    @DatabaseMonitored(repository = "category", method = "getUncategorizedServices", operation = "select")
    override fun getUncategorizedServices(companyId: Long): List<UncategorizedService> {
        logger.debug("Fetching uncategorized services for company: {}", companyId)

        val projections = statisticsJpaRepository.findUncategorizedServices(companyId)

        val services = projections.map { projection ->
            UncategorizedService(
                id = ServiceId(projection.getServiceId()),
                name = projection.getServiceName(),
                servicesCount = projection.getServicesCount(),
                totalRevenue = projection.getTotalRevenue()
            )
        }

        logger.debug("Found {} uncategorized services", services.size)
        return services
    }

    @DatabaseMonitored(repository = "category", method = "getCategorizedServices", operation = "select")
    override fun getCategorizedServices(categoryId: CategoryId, companyId: Long): List<CategorizedService> {
        logger.debug("Fetching categorized services for category: {} and company: {}", categoryId.id, companyId)

        val projections = statisticsJpaRepository.findCategorizedServices(categoryId.id, companyId)

        val services = projections.map { projection ->
            CategorizedService(
                id = projection.getServiceId(),
                name = projection.getServiceName(),
                servicesCount = projection.getServicesCount(),
                totalRevenue = projection.getTotalRevenue()
            )
        }

        logger.debug("Found {} categorized services", services.size)
        return services
    }

    @DatabaseMonitored(repository = "category", method = "getCategoryStatsSummary", operation = "select")
    override fun getCategoryStatsSummary(categoryId: CategoryId, companyId: Long): CategoryStatsSummary {
        logger.debug("Fetching category stats summary for category: {} and company: {}", categoryId.id, companyId)

        val projection = statisticsJpaRepository.findCategoryStatsSummary(categoryId.id, companyId)
            ?: throw IllegalArgumentException("Category not found: ${categoryId.id}")

        val summary = CategoryStatsSummary(
            categoryId = categoryId,
            categoryName = projection.getCategoryName(),
            totalOrders = projection.getTotalOrders(),
            totalRevenue = projection.getTotalRevenue(),
            servicesCount = projection.getServicesCount().toInt()
        )

        logger.debug("Retrieved summary for category: {}", categoryId.id)
        return summary
    }

    @DatabaseMonitored(repository = "category", method = "getCategoryStatsTimeSeries", operation = "select")
    override fun getCategoryStatsTimeSeries(
        categoryId: CategoryId,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity,
        companyId: Long
    ): List<TimeSeriesData> {
        logger.debug("Fetching category time series for category: {} from {} to {} with granularity: {}",
            categoryId.id, startDate, endDate, granularity)

        // Używamy poprawnej metody do mapowania granularity
        val granularityStr = TimeSeriesMapper.getDateTruncUnit(granularity)
        val dateFormat = TimeSeriesMapper.getDateFormatString(granularity)

        val projections = statisticsJpaRepository.findCategoryStatsTimeSeries(
            categoryId.id,
            startDate,
            endDate,
            granularityStr,
            dateFormat,
            companyId
        )

        val timeSeries = projections.map { projection ->
            TimeSeriesMapper.mapToTimeSeriesData(projection, granularity)
        }

        logger.debug("Found {} time series data points", timeSeries.size)
        return timeSeries
    }

    @DatabaseMonitored(repository = "category", method = "getServiceStats", operation = "select")
    override fun getServiceStats(
        serviceId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        granularity: TimeGranularity,
        companyId: Long
    ): ServiceStatsData {
        logger.debug("Fetching service stats for service: {} from {} to {} with granularity: {}",
            serviceId, startDate, endDate, granularity)

        val serviceNameProjection = statisticsJpaRepository.findServiceName(serviceId, companyId)
            ?: throw IllegalArgumentException("Service not found: $serviceId")

        // Używamy poprawnej metody do mapowania granularity
        val granularityStr = TimeSeriesMapper.getDateTruncUnit(granularity)
        val dateFormat = TimeSeriesMapper.getDateFormatString(granularity)

        val projections = statisticsJpaRepository.findServiceStatsTimeSeries(
            serviceId,
            startDate,
            endDate,
            granularityStr,
            dateFormat,
            companyId
        )

        val timeSeries = projections.map { projection ->
            TimeSeriesMapper.mapToTimeSeriesData(projection, granularity)
        }

        logger.debug("Found {} time series data points for service", timeSeries.size)

        return ServiceStatsData(
            serviceId = serviceId,
            serviceName = serviceNameProjection.getServiceName(),
            granularity = granularity,
            data = timeSeries
        )
    }
}