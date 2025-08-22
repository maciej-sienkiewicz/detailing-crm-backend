package com.carslab.crm.production.modules.stats.application.service

import com.carslab.crm.production.modules.stats.application.dto.AddToCategoryRequest
import com.carslab.crm.production.modules.stats.application.dto.CategoryResponse
import com.carslab.crm.production.modules.stats.application.dto.CreateCategoryRequest
import com.carslab.crm.production.modules.stats.domain.StatsService
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import com.carslab.crm.production.modules.stats.domain.model.ServiceId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StatsCommandService(
    private val statsService: StatsService
) {
    private val logger = LoggerFactory.getLogger(StatsCommandService::class.java)

    fun createCategory(request: CreateCategoryRequest): CategoryResponse {
        logger.info("Creating category: {}", request.name)

        val category = statsService.createCategory(request.name)

        logger.info("Category created successfully: {}", category.id.id)
        return CategoryResponse.from(category)
    }

    fun addToCategory(categoryId: Long, request: AddToCategoryRequest) {
        logger.info("Adding {} services to category: {}", request.serviceIds.size, categoryId)

        val serviceIds = request.serviceIds.map { ServiceId(it) }
        statsService.addToCategory(serviceIds, CategoryId(categoryId))

        logger.info("Services added to category successfully")
    }
}