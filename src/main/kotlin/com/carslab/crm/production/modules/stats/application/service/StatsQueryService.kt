package com.carslab.crm.production.modules.stats.application.service

import com.carslab.crm.production.modules.stats.application.dto.UncategorizedServiceResponse
import com.carslab.crm.production.modules.stats.domain.StatsService
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
}